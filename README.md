# Enterprise Integration Architecture: B2B Order-to-Cash (OTC) EDIFACT Ingestion via TPM & ISA-M to SAP S/4HANA & SFTP DLQ

[![SAP Integration Suite](https://img.shields.io/badge/SAP%20Integration%20Suite-Cloud%20Integration-0A85EA?style=for-the-badge&logo=sap&logoColor=white)](https://www.sap.com)
[![Trading Partner Management](https://img.shields.io/badge/B2B-Trading%20Partner%20Management%20(TPM)-0070F2?style=for-the-badge&logo=sap&logoColor=white)](https://www.sap.com)
[![Integration Advisor](https://img.shields.io/badge/ISA--M-Integration%20Advisor%20MAG%2FMIG-green?style=for-the-badge&logo=sap&logoColor=white)](https://www.sap.com)
[![EDI Standard](https://img.shields.io/badge/EDI-UN%2FEDIFACT%20D.01B%20ORDERS-orange?style=for-the-badge)](https://unece.org)
[![S/4HANA SD](https://img.shields.io/badge/Target-S%2F4HANA%20ORDERS05%20IDoc-8A2BE2?style=for-the-badge&logo=sap&logoColor=white)](https://www.sap.com)
[![Clean Core](https://img.shields.io/badge/Clean%20Core-Standard%20IDoc%20Posting-success?style=for-the-badge&logo=sap&logoColor=white)](https://www.sap.com)
[![Protocol](https://img.shields.io/badge/Protocol-AS2%20%7C%20HTTPS-red?style=for-the-badge)](https://en.wikipedia.org/wiki/AS2)
[![Dead Letter Queue](https://img.shields.io/badge/Resilience-SFTP%20Dead%20Letter%20Queue-lightgrey?style=for-the-badge)](https://help.sap.com)

---

## Executive Summary

This enterprise integration blueprint implements an automated, production-grade **Order-to-Cash (OTC)** B2B inbound ordering pipeline on **SAP Integration Suite (Cloud Integration)**. It replaces legacy on-premise B2Bi/ESB middleware with modern cloud B2B capabilities, utilizing **Trading Partner Management (TPM)** and **Integration Advisor (ISA-M)**.

The architecture ingests cryptographic **UN/EDIFACT D.01B ORDERS** transactions transmitted by global retail distribution partners over **AS2**, validates sender/receiver Global Location Numbers (GLN) against active Trading Partner Agreements (TPA), normalizes EDI segments into canonical structures via in-flight **Groovy** and **XSLT 3.0** mapping guidelines (MAG), posts standard **SALESORDER_CREATEFROMDAT2** (`ORDERS05`) IDocs into **SAP S/4HANA Sales & Distribution (SD)**, and guarantees end-to-end resilience via an automated **SFTP Dead-Letter Queue (DLQ)**.

---

## Architecture Overview

### High-Level Topology

```
[B2B Partner: RetailDistro Global (GLN: 4098765000012)]
       │
       │ AS2 Protocol (PKCS#7 Signed & Encrypted / Synchronous MDN)
       │ EDIFACT D.01B ORDERS Transmission
       ▼
[SAP Integration Suite (Cloud Integration B2B Runtime)]
 │
 ├── Security & Ingestion Layer:
 │    └─ AS2 / HTTPS Inbound Adapter (/b2b/edifact/orders)
 │         ├─ Content Modifier: CM_Inject_B2B_Audit
 │         ├─ Preserves raw UNB/UNH envelope in exchange property
 │         └─ Injects correlation headers (SAP_MessageProcessingLogID)
 │
 ├── TPM Validation & Normalization Pipeline:
 │    └─ Groovy Streaming Engine: ParseEDIFACT_ORDERS.groovy
 │         ├─ Parses UNB, BGM, DTM, NAD, LIN, QTY, PRI segments
 │         ├─ Validates Buyer GLN against Trading Partner Agreement (TPA)
 │         └─ Streams canonical XML structure (<CanonicalOrder>)
 │
 ├── ISA-M Mapping Engine:
 │    └─ XSLT 3.0 Pipeline: ORDERS_to_SALESORDER.xsl
 │         ├─ Maps Canonical XML to S/4HANA IDoc SALESORDER_CREATEFROMDAT2
 │         ├─ Generates EDI_DC40 control record (MESTYP: ORDERS, IDOCTYP: ORDERS05)
 │         └─ Normalizes E1EDK01 (header), E1EDKA1 (partners), and E1EDP01 (line items)
 │
 ├── ERP Outbound Dispatch:
 │    └─ Groovy Script: Prepare_S4HANA_IDoc.groovy
 │         ├─ Enriches IDoc XML transport headers
 │         └─ IDoc / SOAP / RFC Adapter -> SAP S/4HANA Cloud SD Module (VA01 Posting)
 │
 └── Exception Subprocess (SFTP Dead-Letter Governance):
      ├─ Catches syntax validation errors or ERP transport downtime
      ├─ Groovy Formatter: Format_B2B_DLQ_Error.groovy
      └─ Outbound SFTP Adapter -> /dlq/orders/failed/ (Quarantine & Partner Alerting)
```

---

## Technical Specifications

| Component | Technical Implementation | Architectural Justification |
| :--- | :--- | :--- |
| **Inbound Adapter** | AS2 / HTTPS Adapter (`/b2b/edifact/orders`) | Non-repudiation, payload encryption, and synchronous MDN generation matching B2B enterprise standards. |
| **Audit Context** | Content Modifier (`CM_Inject_B2B_Audit`) | Captures raw payload and correlates upstream purchase orders with BTP MPL tracking GUIDs. |
| **B2B Splitting & Parsing** | Groovy Engine (`ParseEDIFACT_ORDERS.groovy`) | High-throughput streaming parser extracting EDIFACT D.01B segments without JVM heap overhead. |
| **B2B Governance** | Trading Partner Management (TPM) | Centralizes partner profiles, GLN routing identifiers, and agreement choreographies. |
| **Transformation (ISA-M)** | XSLT 3.0 (`ORDERS_to_SALESORDER.xsl`) | Native rule-based transformation mapping canonical order trees to standard SAP IDoc structures. |
| **Outbound ERP** | IDoc / SOAP Adapter | Creates sales orders directly within SAP S/4HANA SD (`SALESORDER_CREATEFROMDAT2 / ORDERS05`). |
| **Clean Core Compliance** | Standard `ORDERS05` Basic Type | Eliminates custom Z-tables and deprecated BAPIs, ensuring cloud-upgrade readiness. |
| **DLQ Governance** | Exception Subprocess (`Format_B2B_DLQ_Error.groovy`) | Isolates corrupted transactions into an SFTP dead-letter repository, preventing pipeline halts. |

---

## Trading Partner Management (TPM) Configuration

The B2B directory in SAP Integration Suite establishes governance boundaries between trading partners:

* **Own Company Profile:** `GlobalEnterprise SE`
  * **GLN:** `7612345000018`
  * **AS2 Identifier:** `GLOBAL_SE_AS2`
  * **Role:** Supplier / Manufacturer (Order Receiver)
* **Trading Partner Profile:** `RetailDistro Global`
  * **GLN:** `4098765000012`
  * **AS2 Identifier:** `RETAIL_DIST_AS2`
  * **Role:** Customer / Buyer (Order Originator)
* **Trading Partner Agreement (TPA):**
  * **Transaction:** Inbound Purchase Order (`ORDERS`)
  * **Choreography:** Inbound EDIFACT D.01B ──► S/4HANA Sales Order IDoc ──► Synchronous MDN + Outbound CONTRL Acknowledgment.

---

## Message Mapping Details (EDIFACT D.01B ORDERS to SAP IDoc)

| EDIFACT Segment & Element | Target S/4HANA IDoc Field | Business Semantic & Rule |
| :--- | :--- | :--- |
| `BGM+220+<PO_NUM>+9'` | `E1EDK01-BELNR` | Customer Purchase Order Reference Number |
| `DTM+137:<ORDER_DATE>:102'` | `E1EDK01-EDATUV` | Document / Order Date (Format: CCYYMMDD) |
| `NAD+BY+<BUYER_GLN>::9'` | `E1EDKA1-PARTN (PARVW = 'AG')` | Sold-to Party Global Location Number (GLN) |
| `NAD+SU+<SUPPLIER_GLN>::9'` | `EDI_DC40-RCVPRN` | Vendor / Supplier Receiving Identification |
| `LIN+<LINE_NUM>++<SKU>:EN'` | `E1EDP19-KTEXT (QUALF = '002')` | Line Item Material Master SKU / EAN |
| `QTY+21:<QUANTITY>:PCE'` | `E1EDP01-MENGE` | Ordered Quantity in discrete Units of Measure |
| `PRI+AAA:<PRICE>:CT:NTP'` | `E1EDP01-VPREI` | Net Unit Price calculation base |
| `UNS+S'` | Section Separator | Structural divider separating line item detail from summary totals |
| `UNT+<COUNT>+<REF>'` | Control Record | Message trailer count validating transmission segment integrity |

---

## Dead-Letter Queue (DLQ) & Resilience Handling

In the event of network disruption, schema mismatch, or missing trading partner agreements, the flow intercepts the fault:

1. **Interception:** The **Exception Subprocess** catches runtime exceptions (`CamelExceptionCaught`).
2. **Envelope Generation:** `Format_B2B_DLQ_Error.groovy` encapsulates the failure into a structured operational incident envelope containing the original EDI message, buyer GLN, purchase order reference, and correlation ID.
3. **Quarantine Dispatch:** The payload is routed via SFTP adapter to `/dlq/orders/failed/` using dynamic naming:
   `FAILED_ORDERS_${property.B2B_Order_Ref}_${header.SAP_MessageProcessingLogID}.json`
4. **Partner Alerting:** Operational teams receive immediate notification with correlation IDs for rapid reconciliation.

---

## Repository Structure

```
├── README.md                                       # Comprehensive architectural documentation
├── ADR-001-B2B-EDIFACT-TPM-Architecture.md         # Architecture Decision Record
├── IF_B2B_EDIFACT_ORDERS_to_S4HANA_SALESORDER.zip  # Production iFlow runtime bundle export
├── ParseEDIFACT_ORDERS.groovy                      # High-throughput EDIFACT D.01B streaming parser
├── ORDERS_to_SALESORDER.xsl                        # XSLT 3.0 mapping engine to ORDERS05 IDoc
├── Prepare_S4HANA_IDoc.groovy                      # IDoc header enrichment script
├── Format_B2B_DLQ_Error.groovy                     # Dead-letter queue incident formatter
├── EDIFACT_D01B_ORDERS_Sample.edi                  # Production-valid EDIFACT D.01B ORDERS payload
└── .gitignore                                      # Standard Git exclusions
```

---

## Verification & Deployment Runbook

### 1. Verification of Runtime Endpoint
Confirm the integration flow status in **Monitor** ──► **Manage Integration Content**:
```text
https://<tenant-runtime>.cfapps.<region>[.hana.ondemand.com/http/b2b/edifact/orders](https://.hana.ondemand.com/http/b2b/edifact/orders)
```

### 2. Dispatched cURL Test
Execute an authorized POST request using the sample EDI payload:

```bash
curl -X POST \
  'https://<tenant-runtime>.cfapps.<region>[.hana.ondemand.com/http/b2b/edifact/orders](https://.hana.ondemand.com/http/b2b/edifact/orders)' \
  -H 'Content-Type: text/plain' \
  -H 'Authorization: Bearer <BTP_OAUTH_TOKEN>' \
  --data-binary '@EDIFACT_D01B_ORDERS_Sample.edi'
```

### 3. Monitoring Trace
* Inspect execution steps in **Monitor Message Processing**.
* Verify successful canonical parsing, XSLT execution, and IDoc payload generation.
* Ensure synchronous response returns `S4HANA_IDoc_Response` with status `SUCCESS`.

---

## Author

**Željko Popov**  
*Integration Architect | SAP Integration Suite & Clean Core Specialist*  
[GitHub Repositories](https://github.com/neurozeljko)
