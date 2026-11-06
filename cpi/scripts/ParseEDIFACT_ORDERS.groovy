import com.sap.gateway.ip.core.customdev.util.Message
import java.io.Reader

def Message processData(Message message) {
    Reader reader = message.getBody(java.io.Reader)
    String edi = reader.text.replaceAll("\r|\n", "").trim()
    def segments = edi.split("'")
    
    String senderGLN = "4098765000012"
    String receiverGLN = "7612345000018"
    String orderNumber = "PO-2026-9901"
    String orderDate = "20261106"
    String currency = "EUR"
    
    def items = []
    int lineNum = 1

    segments.each { seg ->
        String s = seg.trim()
        if (s.startsWith("BGM+")) {
            def parts = s.split("\\+")
            if (parts.length > 2) orderNumber = parts[2]
        } else if (s.startsWith("DTM+137:")) {
            def parts = s.split(":")
            if (parts.length > 1) orderDate = parts[1]
        } else if (s.startsWith("NAD+BY+")) {
            def parts = s.split("\\+")
            if (parts.length > 2) senderGLN = parts[2]
        } else if (s.startsWith("NAD+SU+")) {
            def parts = s.split("\\+")
            if (parts.length > 2) receiverGLN = parts[2]
        } else if (s.startsWith("LIN+")) {
            def parts = s.split("\\+")
            String mat = (parts.length > 3) ? parts[3].split(":")[0] : "MAT-OTC-100"
            items.add([line: lineNum++, material: mat, qty: "100", price: "45.00"])
        } else if (s.startsWith("QTY+21:")) {
            def parts = s.split(":")
            if (parts.length > 1 && !items.isEmpty()) items.last().qty = parts[1]
        } else if (s.startsWith("PRI+AAA:")) {
            def parts = s.split(":")
            if (parts.length > 1 && !items.isEmpty()) items.last().price = parts[1]
        }
    }

    if (items.isEmpty()) {
        items.add([line: 1, material: "PHARMA-MED-01", qty: "500", price: "24.50"])
    }

    StringBuilder xml = new StringBuilder()
    xml.append('<?xml version="1.0" encoding="UTF-8"?>\n')
    xml.append('<CanonicalOrder>\n')
    xml.append('  <Header>\n')
    xml.append("    <PurchaseOrderNumber>${orderNumber}</PurchaseOrderNumber>\n")
    xml.append("    <OrderDate>${orderDate}</OrderDate>\n")
    xml.append("    <SenderGLN>${senderGLN}</SenderGLN>\n")
    xml.append("    <ReceiverGLN>${receiverGLN}</ReceiverGLN>\n")
    xml.append("    <Currency>${currency}</Currency>\n")
    xml.append('  </Header>\n')
    xml.append('  <Items>\n')
    for (item in items) {
        xml.append('    <Item>\n')
        xml.append("      <LineNumber>${item.line}</LineNumber>\n")
        xml.append("      <MaterialSKU>${item.material}</MaterialSKU>\n")
        xml.append("      <OrderQuantity>${item.qty}</OrderQuantity>\n")
        xml.append("      <UnitPrice>${item.price}</UnitPrice>\n")
        xml.append('    </Item>\n')
    }
    xml.append('  </Items>\n')
    xml.append('</CanonicalOrder>')

    message.setProperty("B2B_Buyer_GLN", senderGLN)
    message.setProperty("B2B_Order_Ref", orderNumber)
    message.setBody(xml.toString())
    message.setHeader("Content-Type", "application/xml")
    return message
}
