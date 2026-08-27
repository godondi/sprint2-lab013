## Vulnerabilities

1. Batch Payout Job - Mishandling of Exceptional Conditions

The method runNightlyBatch fails to distinguish a payment that was never attempted from a payment that was attempted but failed while the transfer was in progress. This can be seen on line 31 as approval status is set to 'PAID' even if the transfer failed for payout.


2. Merchant Controller - Broken Access Control

The method getPayout allows for any user to request by ID regardless of whether the user is the merchant or not. This is due to a lack of authentication.


3. Payout Approval Service - Insecure Design

The method approve allows for payouts to be approved by the person requesting them. This is a prime example of insecure design as the approval process fails to prevent the approver from being the requester as well.


4. Webhook Controller - Software or Data Integrity Failures / Broken Access Control

There is a complete lack of verification in this method as anyone who can reach this endpoint can mark a payment's status as settled regardless of whether or not they are actually the provider for the payment.