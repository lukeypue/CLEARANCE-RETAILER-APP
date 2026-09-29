import copy, importlib.util, pathlib, unittest, tempfile, json, time, io, contextlib
from unittest.mock import patch
spec=importlib.util.spec_from_file_location('collector',pathlib.Path(__file__).parents[1]/'scripts'/'collect-feed.py')
c=importlib.util.module_from_spec(spec);spec.loader.exec_module(c)
class FeedTests(unittest.TestCase):
 def setUp(self):
  self.store={'id':'2921','retailer':'walmart','name':'Harrisville','address':'534 N Harrisville Rd','distance':2.9}
  self.product={'id':'123','title':'Kids pajamas','price':10,'currency':'USD','seller_name':'Walmart.com','out_of_stock':False,'fulfillment':{'pickup':True},'url':'/ip/pajamas/123'}
  self.payload={'location':{'store_id':'2921'},'products':[self.product]}
 def parse(self):return c.normalize_walmart(self.payload,self.store,1789810000000)
 def test_wrong_store_rejected(self):
  self.payload['location']['store_id']='3789'
  with self.assertRaises(ValueError):self.parse()
 def test_deduplicates_same_store_product(self):
  self.payload['products']*=2;self.assertEqual(len(self.parse()),1)
 def test_shipping_seller_and_sold_out_excluded(self):
  for change in [{'seller_name':'third party'},{'out_of_stock':True},{'fulfillment':{'pickup':False}}]:
   self.payload['products']=[dict(self.product,**change)];self.assertEqual(self.parse(),[])
 def test_bad_prices_excluded(self):
  for price in [-1,0,True,float('nan'),'unknown',1.001]:
   self.payload['products']=[dict(self.product,price=price)];self.assertEqual(self.parse(),[])
 def test_does_not_invent_discount_or_inventory(self):
  x=self.parse()[0];self.assertEqual(x['price_cents'],1000);self.assertIsNone(x['original_cents']);self.assertNotIn('quantity',x);self.assertEqual(x['observed_at'],1789810000000)
 def test_unsafe_url_excluded(self):
  self.product['url']='https://evil.example/ip/123';self.assertEqual(self.parse(),[])
 def test_null_fulfillment_is_excluded_without_losing_valid_products(self):
  self.payload['products']=[dict(self.product,fulfillment=None),dict(self.product,id='124')]
  self.assertEqual([offer['product_id'] for offer in self.parse()],['124'])
 def test_missing_products_is_not_empty_success(self):
  self.payload.pop('products')
  with self.assertRaises(ValueError):self.parse()
 def test_budget_reserve(self):
  with self.assertRaises(ValueError):c.check_budget({'max_api_credit':1000,'used_api_credit':871})
  c.check_budget({'max_api_credit':1000,'used_api_credit':870})
 def test_online_card_scope(self):
  h='<div data-testid="product-pod"><a href="/p/Test-Tool/123"><img alt="Test tool"></a><div data-component="price:Price:v6"><span>$</span><span>99</span><span>.</span><span>00</span><span>$219.00</span></div></div>'
  x=c.normalize_home_depot(h,1789810000000)[0];self.assertEqual(x['scope'],'online');self.assertEqual(x['store_id'],'');self.assertEqual(x['price_cents'],9900);self.assertIsNone(x['original_cents'])
 def test_challenge_not_a_catalog(self):
  with self.assertRaises(ValueError):c.normalize_home_depot('<title>Challenge Validation</title>',1789810000000)
 def test_usage_reporting_failure_keeps_successful_collection(self):
  responses=[{'max_api_credit':1000,'used_api_credit':200}]+[{'location':{'store_id':s['id']},'products':[self.product]} for s in c.STORES]+[OSError('usage unavailable')]
  with tempfile.TemporaryDirectory() as folder, patch.object(c,'request',side_effect=responses), patch('builtins.print'):
   path=pathlib.Path(folder)/'feed.json';path.write_text('{"offers":[]}')
   c.collect(path,'test-key')
   self.assertEqual(len(json.loads(path.read_text())['offers']),3)
 def test_partial_collection_failure_preserves_previous_bytes(self):
  responses=[{'max_api_credit':1000,'used_api_credit':200},self.payload,OSError('collection unavailable')]
  with tempfile.TemporaryDirectory() as folder, patch.object(c,'request',side_effect=responses):
   path=pathlib.Path(folder)/'feed.json';previous='{"offers":[]}';path.write_text(previous)
   with self.assertRaises(OSError):c.collect(path,'test-key')
   self.assertEqual(path.read_text(),previous)
class CollectionCacheTests(unittest.TestCase):
 def setUp(self):
  self.now=int(time.time()*1000)
  self.product={'id':'123','title':'Kids pajamas','price':10,'currency':'USD','seller_name':'Walmart.com','out_of_stock':False,'fulfillment':{'pickup':True},'url':'/ip/pajamas/123'}
  self.online={'id':'home-depot:online:456','retailer':'home-depot','store_id':'','product_id':'456','title':'Test tool','category':'Tools','price_cents':9900,'original_cents':None,'pickup':False,'observed_at':self.now-86400000,'scope':'online','url':'https://www.homedepot.com/p/Test-Tool/456'}
  self.previous={'schema':1,'zip':'84414','generated_at':self.now,'sources':copy.deepcopy(c.SOURCES),'stores':copy.deepcopy(c.STORES),'offers':[self.offer('2921'),self.offer('3789'),self.offer('1708'),self.online]}
 def offer(self,store_id):
  return {'id':'walmart:'+store_id+':321','retailer':'walmart','store_id':store_id,'product_id':'321','title':'Previous pajamas','category':'Clothing','price_cents':1500,'original_cents':None,'pickup':True,'observed_at':self.now-3600000,'scope':'store_pickup','url':'https://www.walmart.com/ip/previous-pajamas/321'}
 def scans(self,**times):
  self.previous['store_scans']={'walmart:'+store_id:{'last_successful_scan_at':stamp} for store_id,stamp in times.items()}
 def response(self,store_id,products=None):
  return {'location':{'store_id':store_id},'products':[self.product] if products is None else products}
 def test_plan_legacy_feed_does_not_use_generation_or_offer_times(self):
  result=c.collection_plan(self.previous,self.now)
  self.assertEqual(result['due_store_ids'],['2921','3789','1708'])
  self.assertEqual(result['cached_store_ids'],[])
  self.assertEqual(result['search_requests'],3)
  self.assertEqual(result['estimated_credits'],30)
  self.assertEqual(result['minimum_remaining_credits'],130)
 def test_plan_six_hour_boundary_and_mixed_store_cache(self):
  self.scans(**{'2921':self.now-1000,'3789':self.now-21600000,'1708':self.now-21599999})
  result=c.collection_plan(self.previous,self.now)
  self.assertEqual(result['due_store_ids'],['3789'])
  self.assertEqual(result['cached_store_ids'],['2921','1708'])
  self.assertEqual(result['search_requests'],1)
  self.assertEqual(result['estimated_credits'],10)
  self.assertEqual(result['minimum_remaining_credits'],110)
 def test_future_and_malformed_scan_metadata_never_claim_freshness(self):
  for stamp in [self.now+1,True,None,str(self.now),float(self.now),-1]:
   with self.subTest(stamp=stamp):
    self.scans(**{'2921':stamp})
    self.assertIn('2921',c.collection_plan(self.previous,self.now)['due_store_ids'])
  for malformed in [None,[],{'walmart:2921':None},{'walmart:2921':self.now}]:
   with self.subTest(metadata=malformed):
    self.previous['store_scans']=malformed
    self.assertEqual(c.collection_plan(self.previous,self.now)['search_requests'],3)
 def test_plan_cli_needs_no_key_network_or_file_write(self):
  with tempfile.TemporaryDirectory() as folder:
   path=pathlib.Path(folder)/'feed.json';previous=json.dumps(self.previous);path.write_text(previous)
   output=io.StringIO()
   with patch.dict(c.os.environ,{},clear=True),patch.object(c,'request',side_effect=AssertionError('Offline planning must not request data')),contextlib.redirect_stdout(output):
    c.main(['--plan','--output',str(path)])
   result=json.loads(output.getvalue())
   self.assertEqual(result['estimated_credits'],30)
   self.assertTrue(result['assumptions'])
   self.assertEqual(path.read_text(),previous)
 def test_plan_missing_output_does_not_create_it(self):
  with tempfile.TemporaryDirectory() as folder:
   path=pathlib.Path(folder)/'missing'/'feed.json';output=io.StringIO()
   with patch.dict(c.os.environ,{},clear=True),patch.object(c,'request',side_effect=AssertionError('Offline planning must not request data')),contextlib.redirect_stdout(output):
    c.main(['--plan','--output',str(path)])
   self.assertEqual(json.loads(output.getvalue())['search_requests'],3)
   self.assertFalse(path.parent.exists())
 def test_fresh_collection_makes_no_requests_or_write(self):
  self.scans(**{store_id:self.now-1000 for store_id in ['2921','3789','1708']})
  with tempfile.TemporaryDirectory() as folder,patch.object(c,'request',side_effect=AssertionError('Cached collection must not request data')):
   path=pathlib.Path(folder)/'feed.json';previous=json.dumps(self.previous,indent=3);path.write_text(previous);mtime=path.stat().st_mtime_ns
   with contextlib.redirect_stdout(io.StringIO()):c.collect(path,'unused-key')
   self.assertEqual(path.read_text(),previous)
   self.assertEqual(path.stat().st_mtime_ns,mtime)
 def test_only_due_store_is_refreshed_and_cached_observations_survive(self):
  self.scans(**{'2921':self.now-1000,'3789':self.now-1000,'1708':self.now-21600000})
  responses=[{'max_api_credit':1000,'used_api_credit':890},self.response('1708'),{'max_api_credit':1000,'used_api_credit':900}]
  with tempfile.TemporaryDirectory() as folder,patch.object(c,'request',side_effect=responses) as network,contextlib.redirect_stdout(io.StringIO()):
   path=pathlib.Path(folder)/'feed.json';path.write_text(json.dumps(self.previous));c.collect(path,'test-key');saved=json.loads(path.read_text())
   self.assertEqual([call.args[2]['store_id'] for call in network.call_args_list if call.args[1]=='walmart/search'],['1708'])
   for store_id in ['2921','3789']:
    self.assertEqual([x for x in saved['offers'] if x['store_id']==store_id],[self.offer(store_id)])
    self.assertEqual(saved['store_scans']['walmart:'+store_id],self.previous['store_scans']['walmart:'+store_id])
   self.assertEqual([x for x in saved['offers'] if x['scope']=='online'],[self.online])
   refreshed=[x for x in saved['offers'] if x['store_id']=='1708']
   self.assertEqual([x['product_id'] for x in refreshed],['123'])
   self.assertGreaterEqual(refreshed[0]['observed_at'],self.now)
   self.assertEqual(saved['store_scans']['walmart:1708']['last_successful_scan_at'],refreshed[0]['observed_at'])
 def test_due_store_budget_stops_before_any_paid_search(self):
  self.scans(**{'2921':self.now-1000,'3789':self.now-1000})
  with tempfile.TemporaryDirectory() as folder,patch.object(c,'request',side_effect=[{'max_api_credit':1000,'used_api_credit':891}]) as network:
   path=pathlib.Path(folder)/'feed.json';previous=json.dumps(self.previous);path.write_text(previous)
   with self.assertRaises(ValueError):c.collect(path,'test-key')
   self.assertEqual([call.args[1] for call in network.call_args_list],['usage'])
   self.assertEqual(path.read_text(),previous)
 def test_successful_empty_scans_are_cached(self):
  responses=[{'max_api_credit':1000,'used_api_credit':200}]+[self.response(store_id,[]) for store_id in ['2921','3789','1708']]+[{'max_api_credit':1000,'used_api_credit':230}]
  with tempfile.TemporaryDirectory() as folder,patch.object(c,'request',side_effect=responses),contextlib.redirect_stdout(io.StringIO()):
   path=pathlib.Path(folder)/'feed.json';path.write_text(json.dumps(self.previous));c.collect(path,'test-key');saved=json.loads(path.read_text())
   self.assertEqual(saved['offers'],[self.online])
   self.assertEqual(set(saved['store_scans']),{'walmart:2921','walmart:3789','walmart:1708'})
   with patch.object(c,'request',side_effect=AssertionError('Empty successful scans must be reused')):c.collect(path,'test-key')
   self.assertEqual(json.loads(path.read_text()),saved)
 def test_partial_failure_does_not_publish_successful_store_timestamps(self):
  self.scans(**{'2921':self.now-1000})
  responses=[{'max_api_credit':1000,'used_api_credit':200},self.response('3789'),OSError('collection unavailable')]
  with tempfile.TemporaryDirectory() as folder,patch.object(c,'request',side_effect=responses):
   path=pathlib.Path(folder)/'feed.json';previous=json.dumps(self.previous,indent=3);path.write_text(previous)
   with self.assertRaises(OSError):c.collect(path,'test-key')
   self.assertEqual(path.read_text(),previous)
if __name__=='__main__':unittest.main()
