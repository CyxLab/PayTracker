# PayTrack Phase 1 – Acceptance Criteria

Document Reference:LV-JAVA-PAYTRACK-P1  
Section: 6 – Acceptance Criteria  
Version: 1.0


# AC-1.1 – Tenant Isolation

Acceptance Criterion:  
Each distributor's data must remain private. A user from one distributor must not be able to access another distributor's invoices.

Verification:  
Run an automated test to confirm that a user belonging to one distributor cannot read another distributor's invoices.

# AC-1.2 – Safe Invoice Numbering

Acceptance Criterion:  
Invoice numbers must remain unique and sequential, even when multiple invoices are issued simultaneously.

Verification: 
Issue 20 invoices in parallel and verify that all 20 receive consecutive numbers without gaps or duplicates.

# AC-1.3 – Correct Tax Calculation

Acceptance Criterion:  
The system must calculate GST correctly for invoices involving different locations and tax rates.

Verification: 
A reviewer checks three invoices (same state, other state, and mixed tax rates) against manual calculations.

# AC-1.4 – Change Request CR-1

Acceptance Criterion: 
The recurring invoice feature requested in CR-1 must be implemented and available in production.

Verification:  
Demonstrate the feature on production and include the impact estimate in the release notes.

# AC-1.5 – Client-Specific Requirement

Acceptance Criterion: 
The additional requirement assigned to the team's client must be implemented successfully.

Verification: 
Demonstrate the implementation during the project review and provide at least one automated test covering it.



