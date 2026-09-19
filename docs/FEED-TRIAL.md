# Six-retailer feed feasibility trial — 2026-09-19

## Outcome

Authenticated ScrapingBee trial calls completed for all six retailers in the app. The final usage endpoint reported 1,000 total credits, 163 used, 837 remaining, and zero requests running. No paid subscription was purchased. This was a backend feasibility experiment, not a new Android build or a deployed feed.

The test location for Walmart was American Fork, Utah, store 2511, ZIP 84003, near the existing 84043 development fixture. This is not the user's confirmed trial ZIP. The other retailer checks were discovery tests without a confirmed local-store selection.

| Retailer | Observed response | Missing before an in-store clearance feed |
| --- | --- | --- |
| Walmart | Search for clearance with store_id=2511 returned 70 entries / 66 distinct item IDs; response location matched store 2511. Three entries had pickup=true. | Complete coverage, shelf prices/quantities, item-level clearance evidence, and variant/store validation. |
| Target | Both basic HTML and rendered clearance-page requests returned site navigation but no product listings. | A reliable product-list extraction path and local-store validation. |
| Home Depot | Clearance search resolved to Savings Center with 24 distinct product links and displayed prices/discounts. The page selected Santa Clara by default. | Local store selection and distinguishing ordinary promotions from clearance. |
| Lowe's | The Back Aisle page returned 24 structured product records; 16 contained a schema.org offer price. Visible cards said to find a store for pricing/availability. | Store selection, reconciliation of embedded prices with visible listings, and clearance evidence. |
| Tractor Supply | Initial response was a short landing page; premium rendered request failed with provider HTTP 500 / upstream 613. | A reliable product-list response, followed by store/price/stock validation. |
| Walgreens | Basic and premium rendered responses returned a Challenge Validation page despite HTTP 200. | A reliable product-list response, followed by store/price/stock validation. |

## Walmart details

- Use the provider's documented search endpoint with query=clearance and store_id. The backend can run these discovery queries automatically; shoppers need not guess item names.
- A baseline request without a store defaulted to Houston store 4416. Never assign a response to a requested store without checking the returned location.
- Adding fulfillment_type=in_store produced HTTP 500 in the tested combinations, including with store_id alone. A store-only request succeeded. This suggests a filter-specific issue in these tests; it does not establish a permanent provider limitation.
- Most results were third-party shipping listings. Only 18 of the 70 entries named Walmart.com as seller; three entries indicated pickup.
- A product detail check for item 19626065486 returned a $10 price and pickup availability. It lacked a response store identifier and a struck-through price.
- That product also contained $2.50 variants marked OUT_OF_STOCK. Do not combine a cheap unavailable variant's price with another variant's availability.
- Search responses had no original price or shelf quantity. A clearance search section alone does not establish that every returned product is locally marked down.
- Actual response keys differed from the documentation example: id, products_count, location.zipcode, fulfillment.pickup, and out_of_stock. Integrate against observed, validated responses.

## Next-version constraints

The requested trial scope remains all six retailers. The next phone build must distinguish verified store-specific observations, online-only deals, and unavailable sources. No current result qualifies as complete physical-store inventory.

A production integration still needs a hosted backend with server-side secrets, shared caching, bounded refresh costs, and parsers that reject challenge pages and wrong-store responses. The existing APK remains v0.2.0 with nearby stores and category links; no credentials or live-feed code were added to it.

Keep the API key out of the repository, APK, fixtures, logs, and public workflow inputs. The experiment used a private temporary file outside the repository. Raw downloaded pages are scratch research, not app data or redistribution-ready fixtures.

Provider references: [Walmart API](https://www.scrapingbee.com/documentation/walmart/), [HTML API and usage accounting](https://www.scrapingbee.com/documentation/).
