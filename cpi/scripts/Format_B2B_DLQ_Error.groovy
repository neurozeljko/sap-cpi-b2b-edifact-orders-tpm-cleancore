import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput
import java.time.Instant

def Message processData(Message message) {
    def properties = message.getProperties()
    def ex = properties.get("CamelExceptionCaught")
    
    String rawError = ex ? ex.getMessage() : "EDIFACT Ingestion & Transformation Validation Failure"
    String rawPayload = properties.get("Original_EDI_Payload") ? properties.get("Original_EDI_Payload").toString() : "N/A"
    String buyerGLN = properties.get("B2B_Buyer_GLN") ? properties.get("B2B_Buyer_GLN").toString() : "4098765000012"
    String orderRef = properties.get("B2B_Order_Ref") ? properties.get("B2B_Order_Ref").toString() : "UNKNOWN_PO"
    String corrId = properties.get("B2B_CorrelationID") ? properties.get("B2B_CorrelationID").toString() : UUID.randomUUID().toString()

    def dlqEnvelope = [
        "DLQ_Header": [
            "Category"      : "B2B_EDIFACT_ORDERS_EXCEPTION",
            "Timestamp"     : Instant.now().toString(),
            "CorrelationId" : corrId,
            "SenderGLN"     : buyerGLN,
            "PurchaseOrder" : orderRef,
            "TargetSystem"  : "S4HANA_SD_IDOC"
        ],
        "ErrorDetails": [
            "FaultReason"   : rawError,
            "ActionRequired": "Inspect syntax errors in D.01B segments or update TPM Agreement"
        ],
        "ArchivedRawPayload": rawPayload
    ]

    message.setBody(JsonOutput.prettyPrint(JsonOutput.toJson(dlqEnvelope)))
    message.setHeader("Content-Type", "application/json")
    message.setHeader("B2B_DLQ_Status", "FAILED_ROUTED_TO_SFTP")
    return message
}
