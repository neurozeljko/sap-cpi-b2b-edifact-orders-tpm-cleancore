import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {
    message.setHeader("SAP_MessageType", "ORDERS")
    message.setHeader("SAP_IDocType", "ORDERS05")
    message.setHeader("Content-Type", "application/xml")
    message.setHeader("Accept", "application/xml")
    return message
}
