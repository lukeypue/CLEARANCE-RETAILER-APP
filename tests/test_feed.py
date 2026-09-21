import copy, importlib.util, pathlib, unittest
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
if __name__=='__main__':unittest.main()
