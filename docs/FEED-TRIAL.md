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

## Follow-up: nearest stores around ZIP 84414

The phone trial location is now ZIP 84414. Fresh Zippopotam.us and OpenStreetMap/Overpass lookups found the following nearest mapped locations within 50 miles. Distances are straight-line miles from the ZIP center (41.3112, -111.9689), not driving distances or distances from a home address.

| Retailer | Nearest mapped store | Approximate miles |
| --- | --- | --- |
| Walmart | Harrisville, 534 North Harrisville Road, store 2921 | 2.9 |
| Target | Riverdale, 1135 West Riverdale Road, store 1753 | 9.7 |
| Home Depot | Ogden, 984 Wall Avenue, store 4411 | 4.4 |
| Lowe's | Ogden, 344 North Washington Boulevard, store 2858 | 3.2 |
| Tractor Supply | West Haven, 1985 West 2550 South, store 1951 | 7.1 |
| Walgreens | North Ogden, 2555 N 400 E, store 10820 | 0.5 |

The five nearest mapped Walmarts were Harrisville 2921 (2.9 miles), Ogden 3789 (5.5), Riverdale 1708 (9.5), South Ogden Neighborhood Market 5206 (11.1), and Perry 3454 (12.4). The first three store identities were also checked against Walmart's public store pages.

Three additional ScrapingBee clearance searches, one per nearest Walmart, all returned the requested store ID and 70 entries / 66 distinct product IDs. Walmart-sold entries marked for pickup numbered six in Harrisville, five in Ogden, and five in Riverdale. These remain candidates: no shelf counts or verified local clearance markdowns were established. The provider's city labels for stores 3789 and 1708 differed from the retailer directory; store ID is the matching key.

Those three requests consumed 30 additional credits. The account endpoint then reported 193 used, 807 remaining, and zero running requests. No other retailer was re-scraped in this follow-up.

The fresh map response exposed a proposed Target (OSM way 1493198240) with normal retailer/shop tags plus proposed:building=yes. It incorrectly ranked first under the previous filter. The source filter now rejects that proposed building; a regression test using its actual tags failed before the fix and passed afterward. All 23 domain tests passed. This narrow correction does not guarantee that all map records are complete or up to date.

Map records are copyright [OpenStreetMap contributors](https://www.openstreetmap.org/copyright), under ODbL; ZIP center from [Zippopotam.us](https://api.zippopotam.us/us/84414). Retailer references: [Harrisville Walmart](https://www.walmart.com/store/2921-harrisville-ut), [Ogden Walmart](https://www.walmart.com/store/3789-ogden-ut), [Riverdale Walmart](https://www.walmart.com/store/1708-riverdale-ut), [Riverdale Target](https://www.target.com/sl/riverdale/1753).
