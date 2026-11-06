<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes" encoding="UTF-8"/>

    <xsl:template match="/CanonicalOrder">
        <SALESORDER_CREATEFROMDAT2>
            <IDOC BEGIN="1">
                <EDI_DC40 SEGMENT="1">
                    <TABNAM>EDI_DC40</TABNAM>
                    <DIRECT>2</DIRECT>
                    <IDOCTYP>ORDERS05</IDOCTYP>
                    <MESTYP>ORDERS</MESTYP>
                    <SNDPRT>LS</SNDPRT>
                    <SNDPRN><xsl:value-of select="Header/SenderGLN"/></SNDPRN>
                    <RCVPRT>LS</RCVPRT>
                    <RCVPRN><xsl:value-of select="Header/ReceiverGLN"/></RCVPRN>
                </EDI_DC40>
                <E1EDK01 SEGMENT="1">
                    <CURCY><xsl:value-of select="Header/Currency"/></CURCY>
                    <BELNR><xsl:value-of select="Header/PurchaseOrderNumber"/></BELNR>
                </E1EDK01>
                <E1EDK14 SEGMENT="1">
                    <QUALF>012</QUALF>
                    <ORGID>1000</ORGID>
                </E1EDK14>
                <E1EDKA1 SEGMENT="1">
                    <PARVW>AG</PARVW>
                    <PARTN><xsl:value-of select="Header/SenderGLN"/></PARTN>
                </E1EDKA1>
                <xsl:for-each select="Items/Item">
                    <E1EDP01 SEGMENT="1">
                        <POSEX><xsl:value-of select="LineNumber"/></POSEX>
                        <MENGE><xsl:value-of select="OrderQuantity"/></MENGE>
                        <E1EDP19 SEGMENT="1">
                            <QUALF>002</QUALF>
                            <KTEXT><xsl:value-of select="MaterialSKU"/></KTEXT>
                        </E1EDP19>
                    </E1EDP01>
                </xsl:for-each>
            </IDOC>
        </SALESORDER_CREATEFROMDAT2>
    </xsl:template>
</xsl:stylesheet>
