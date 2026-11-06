# ADR-001: Modernizing B2B EDI Order-to-Cash Ingestion via TPM, ISA-M and Clean Core IDocs

## Status
Accepted

## Context
The enterprise operates global Order-to-Cash (OTC) relationships with wholesale retail distributors. Legacy EDI relied on brittle on-premise B2Bi systems.

## Decision
We implement a modern B2B integration architecture on SAP Integration Suite utilizing Trading Partner Management (TPM), Integration Advisor (ISA-M MAG/MIG), standard ORDERS05 IDocs, and SFTP DLQ resilience.
