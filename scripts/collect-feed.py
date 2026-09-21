"""Bounded server-side trial collector. Never run this inside an Android client."""
import argparse, json, os, re, time, urllib.request, urllib.parse
from decimal import Decimal, InvalidOperation
from html.parser import HTMLParser
from pathlib import Path
STORES=[
 {'id':'2921','retailer':'walmart','name':'Walmart · Harrisville','address':'534 North Harrisville Road, Harrisville, UT 84404','distance':2.9},
 {'id':'3789','retailer':'walmart','name':'Walmart · Ogden','address':'1959 Wall Avenue, Ogden, UT 84401','distance':5.5},
 {'id':'1708','retailer':'walmart','name':'Walmart · Riverdale','address':'4848 South 900 West, Riverdale, UT 84405','distance':9.5}]
SOURCES=[
 {'retailer':'walmart','status':'Store pickup candidates','detail':'Listings from a clearance search. Shelf price, markdown and quantity are unverified.'},
 {'retailer':'target','status':'Listings unavailable','detail':'Trial requests returned navigation without product listings.'},
 {'retailer':'home-depot','status':'Online deals only','detail':'Online promotions; local clearance and store stock are unverified.'},
 {'retailer':'lowes','status':'Local prices unverified','detail':'Products returned, but the page requires a store for usable local prices.'},
 {'retailer':'tractor-supply','status':'Feed unavailable','detail':'The trial did not return a usable product list.'},
 {'retailer':'walgreens','status':'Feed unavailable','detail':'Trial requests returned a verification page instead of listings.'}]
def cents(value):
 if isinstance(value,bool):raise ValueError('Invalid price')
 try:d=Decimal(str(value))*100
 except InvalidOperation:raise ValueError('Invalid price') from None
 if not d.is_finite() or d<=0 or d>100000000 or d!=d.to_integral_value():raise ValueError('Invalid price')
 return int(d)
def category(title):
 t=title.lower()
 for name,pattern in [('Clothing',r'pajama|shirt|hoodie|jacket|bodysuit|boot|shoe|flats|dress|sweater|tee\b'),('Tools',r'tool|drill|saw|wrench|breaker bar|measuring|inverter'),('Appliances',r'washer|dryer|refrigerator|dishwasher|microwave|freezer'),('Outdoor',r'grill|lawn|patio|garden|mower'),('Home',r'mattress|comforter|bedding|chair|table|toilet')]:
  if re.search(pattern,t):return name
 return 'Other'
def safe_url(value,retailer):
 host={'walmart':'www.walmart.com','home-depot':'www.homedepot.com'}[retailer]
 if value.startswith('/') and not value.startswith('//'):value='https://'+host+value
 u=urllib.parse.urlsplit(value)
 if u.scheme!='https' or u.hostname!=host or u.username or u.password or u.port not in (None,443) or not u.path.startswith(('/ip/' if retailer=='walmart' else '/p/')):raise ValueError('Unsafe retailer link')
 return urllib.parse.urlunsplit((u.scheme,u.netloc,u.path,'',''))
def normalize_walmart(payload,store,observed_at):
 if str(payload.get('location',{}).get('store_id'))!=store['id']:raise ValueError('Wrong store response')
 products=payload.get('products')
 if not isinstance(products,list):raise ValueError('Missing products')
 result={}
 for p in products:
  if not isinstance(p,dict):continue
  if p.get('seller_name')!='Walmart.com' or p.get('out_of_stock') is not False or p.get('fulfillment',{}).get('pickup') is not True:continue
  try:
   pid=str(p['id']);title=p['title'].strip()
   if not pid.isdigit() or not title or p.get('currency')!='USD':continue
   price=cents(p['price']);url=safe_url(p['url'],'walmart')
  except (KeyError,ValueError,TypeError,AttributeError):continue
  identity='walmart:'+store['id']+':'+pid
  result[identity]={'id':identity,'retailer':'walmart','store_id':store['id'],'product_id':pid,'title':title,'category':category(title),'price_cents':price,'original_cents':None,'pickup':True,'observed_at':observed_at,'scope':'store_pickup','url':url}
 return list(result.values())
class ProductCards(HTMLParser):
 VOID={'area','base','br','col','embed','hr','img','input','link','meta','param','source','track','wbr'}
 def __init__(self):super().__init__();self.stack=[];self.card=None;self.cards=[];self.price_depth=None
 def handle_starttag(self,tag,attrs):
  a=dict(attrs)
  if tag not in self.VOID:self.stack.append(tag)
  if a.get('data-testid')=='product-pod':self.card={'depth':len(self.stack),'title':'','url':'','prices':[]}
  if self.card is None:return
  if tag=='img' and not self.card['title']:self.card['title']=a.get('alt','')
  if tag=='a' and not self.card['url'] and a.get('href','').startswith('/p/'):self.card['url']=a['href']
  if a.get('data-component','').startswith('price:Price:'):self.price_depth=len(self.stack)
 def handle_endtag(self,tag):
  if tag not in self.stack:return
  depth=len(self.stack)-self.stack[::-1].index(tag)
  if self.card is not None and depth==self.card['depth']:
   self.cards.append(self.card);self.card=None;self.price_depth=None
  if self.price_depth is not None and depth<=self.price_depth:self.price_depth=None
  self.stack=self.stack[:depth-1]
 def handle_data(self,data):
  if self.card is not None and self.price_depth is not None:self.card['prices'].append(data.strip())
def normalize_home_depot(html,observed_at):
 parser=ProductCards();parser.feed(html);offers={}
 for card in parser.cards:
  try:
   url=safe_url(card['url'],'home-depot');pid=url.rsplit('/',1)[-1]
   prices=' '.join(card['prices']);match=re.search(r'\$\s*([\d,]+)\s*\.\s*(\d{2})\b',prices)
   if not match or not pid.isdigit() or not card['title']:continue
   price=cents(match[1].replace(',','')+'.'+match[2])
  except (ValueError,TypeError):continue
  identity='home-depot:online:'+pid
  offers[identity]={'id':identity,'retailer':'home-depot','store_id':'','product_id':pid,'title':card['title'],'category':category(card['title']),'price_cents':price,'original_cents':None,'pickup':False,'observed_at':observed_at,'scope':'online','url':url}
 if not offers:raise ValueError('No usable online product cards')
 return list(offers.values())
def check_budget(usage):
 remaining=int(usage['max_api_credit'])-int(usage['used_api_credit'])
 if remaining<130:raise ValueError('Credit reserve reached; collection not started')
 return remaining
def request(key,endpoint,params=None):
 url='https://app.scrapingbee.com/api/v1/'+endpoint
 if params:url+='?'+urllib.parse.urlencode(params)
 req=urllib.request.Request(url,headers={'Authorization':'Bearer '+key})
 with urllib.request.urlopen(req,timeout=60) as r:
  raw=r.read(2000001)
  if len(raw)>2000000:raise ValueError('Response too large')
  return json.loads(raw)
def collect(path,key):
 # Three fixed 10-credit calls, no caller-supplied URL, no retry loop.
 check_budget(request(key,'usage'))
 previous=json.loads(path.read_text()) if path.exists() else {}
 online=[x for x in previous.get('offers',[]) if x.get('scope')=='online']
 offers=[]
 for store in STORES:
  body=request(key,'walmart/search',{'query':'clearance','store_id':store['id'],'light_request':'true'})
  offers.extend(normalize_walmart(body,store,int(time.time()*1000)))
 feed={'schema':1,'zip':'84414','generated_at':int(time.time()*1000),'sources':SOURCES,'stores':STORES,'offers':offers+online}
 # A partial failed run never overwrites the prior valid published feed.
 path.parent.mkdir(parents=True,exist_ok=True);temp=path.with_suffix('.tmp');temp.write_text(json.dumps(feed,indent=2));temp.replace(path)
 print('Collected',len(offers),'pickup candidates.')
 try:
  usage=request(key,'usage');remaining=int(usage['max_api_credit'])-int(usage['used_api_credit'])
 except Exception:
  print('Feed saved; remaining credit balance is temporarily unavailable.')
 else:print('Provider reports',remaining,'credits remaining.')
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--output',default='feeds/trial.json');args=parser.parse_args()
 key=os.environ.get('SCRAPINGBEE_API_KEY','')
 if not key:raise SystemExit('Set the SCRAPINGBEE_API_KEY repository secret before running collection.')
 try:collect(Path(args.output),key)
 except Exception as exc:raise SystemExit('Collection failed; prior feed preserved. Error type: '+type(exc).__name__) from None
