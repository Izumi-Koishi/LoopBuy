---
type: "manual"
---

### **Campus Second-hand Trading Platform \- Work Breakdown Structure (WBS)**

This document provides a comprehensive work breakdown for the project, structured into Epics, Features, User Stories, and Tasks, based on the provided Software Requirements Specification (SRS).

#### **Epic 1: Account & Authentication System**

* **Feature 1.1: User Authentication (UC-01)**  
  * **Story 1.1.1: User Registration**  
    * **Description**: As a **Visitor**, I want to **create a new account using a username and password** so that **I can access the full functionality of the platform.**  
    * **Task 1.1.1.1**: \[Backend\] Design the User database schema, including fields for username, passwordHash, status, etc.  
    * **Task 1.1.1.2**: \[Backend\] Develop the registration API endpoint, including logic for username uniqueness validation.  
    * **Task 1.1.1.3**: \[Backend\] Implement password salting and hashing using bcrypt (NFR-SEC-02).  
    * **Task 1.1.1.4**: \[Frontend\] Create the registration page UI with input fields for username, password, and password confirmation.  
    * **Task 1.1.1.5**: \[Frontend\] Implement client-side form validation (e.g., password complexity, matching passwords).  
    * **Task 1.1.1.6**: \[Frontend\] Integrate the registration API, handling success (auto-login and redirect) and failure (e.g., username already exists) scenarios.  
  * **Story 1.1.2: User Login**  
    * **Description**: As a **User**, I want to **log in with my username and password** so that **I can access my personal data and platform features.**  
    * **Task 1.1.2.1**: \[Backend\] Develop the login API endpoint to verify username existence and match the password hash.  
    * **Task 1.1.2.2**: \[Backend\] Implement logic to generate a session or JWT upon successful login.  
    * **Task 1.1.2.3**: \[Backend\] Implement error handling for failed logins, such as "Invalid username or password" and "Account is banned."  
    * **Task 1.1.2.4**: \[Backend\] Integrate a rate-limiting mechanism on login attempts to prevent brute-force attacks (related to NFR-SEC-09).  
    * **Task 1.1.2.5**: \[Frontend\] Create the login page UI.  
    * **Task 1.1.2.6**: \[Frontend\] Integrate the login API, handling credential storage and redirection on success, and displaying error messages on failure.  
* **Feature 1.2: User Profile Page (UC-02)**  
  * **Story 1.2.1: View User Profile**  
    * **Description**: As a **Visitor or User**, I want to **view any user's public profile page** so that **I can learn about their reputation and see their items for sale.**  
    * **Task 1.2.1.1**: \[Backend\] Develop an API to fetch a user's public information, including nickname, avatar, rating, and a paginated list of their on-sale products.  
    * **Task 1.2.1.2**: \[Backend\] Implement logic to handle cases where the requested user does not exist.  
    * **Task 1.2.1.3**: \[Frontend\] Create the user profile page UI, displaying user info and the product list.  
    * **Task 1.2.1.4**: \[Frontend\] Implement navigation to the profile page by clicking user avatars/nicknames from product pages or review lists.  
  * **Story 1.2.2: Follow/Unfollow User (Part of UC-12)**  
    * **Description**: As a **logged-in User**, I want to **see a "Follow" or "Unfollow" button on other users' profiles** so that **I can manage my follow list.**  
    * **Task 1.2.2.1**: \[Backend\] Develop the API endpoints for following and unfollowing a user.  
    * **Task 1.2.2.2**: \[Frontend\] Dynamically display the "Follow/Unfollow" button on the user profile and implement the click interaction to call the API.

#### **Epic 2: Product Management & Discovery**

* **Feature 2.1: Product Publishing & Management (UC-03)**  
  * **Story 2.1.1: Publish a New Product**  
    * **Description**: As a **User**, I want to **fill out a form and upload images to publish a new product for review** so that **I can sell my second-hand items.**  
    * **Task 2.1.1.1**: \[Backend\] Design the Product and Category database schemas.  
    * **Task 2.1.1.2**: \[Backend\] Develop the "Submit for Review" API, performing backend validation and setting the product status to PENDING\_REVIEW.  
    * **Task 2.1.1.3**: \[Backend\] Implement the image upload API, enforcing format (JPEG, PNG, WEBP) and size (max 5MB) constraints (NFR-FILE-01).  
    * **Task 2.1.1.4**: \[Frontend\] Create the "Publish Product" page UI with all required fields.  
    * **Task 2.1.1.5**: \[Frontend\] Implement real-time form validation (e.g., title length, price format).  
    * **Task 2.1.1.6**: \[Frontend\] Integrate an image upload component with preview and delete functionality.  
  * **Story 2.1.2: Save Product as Draft**  
    * **Description**: As a **User**, I want to **save my unfinished product listing as a draft** so that **I can continue editing and publish it later.**  
    * **Task 2.1.2.1**: \[Backend\] Develop the "Save as Draft" API, validating only the title and setting the product status to DRAFT.  
    * **Task 2.1.2.2**: \[Frontend\] Add a "Save as Draft" button on the publishing page and implement the API call.  
  * **Story 2.1.3: Manage My Products**  
    * **Description**: As a **User**, I want to **view all my published products on one page and perform actions like edit, delete, or delist based on their status** so that **I can effectively manage my listings.**  
    * **Task 2.1.3.1**: \[Backend\] Develop an API to fetch the "My Products" list, categorized by status.  
    * **Task 2.1.3.2**: \[Backend\] Develop API endpoints for editing, deleting, delisting, and relisting products.  
    * **Task 2.1.3.3**: \[Frontend\] Create the "My Products" management page, using tabs to display products in different states.  
    * **Task 2.1.3.4**: \[Frontend\] Implement the corresponding action buttons (Edit, Delete, etc.) and interaction logic for products based on their status.  
* **Feature 2.2: Product Discovery (UC-04 & UC-05)**  
  * **Story 2.2.1: Browse, Search, and Filter Products**  
    * **Description**: As a **Visitor or User**, I want to **find products using keyword search, category filters, price range filters, and sorting options** so that **I can quickly find items I'm interested in.**  
    * **Task 2.2.1.1**: \[Backend\] Develop a generic product list query API supporting combined filtering (keyword, category, price) and sorting.  
    * **Task 2.2.1.2**: \[Backend\] Implement pagination logic for the API.  
    * **Task 2.2.1.3**: \[Backend\] Implement logic to return an empty list when no results are found.  
    * **Task 2.2.1.4**: \[Frontend\] Create the product list page UI, integrating a search bar, category selector, price inputs, and sorting dropdown.  
    * **Task 2.2.1.5**: \[Frontend\] Implement logic to automatically call the API and refresh the product list when filter conditions change.  
    * **Task 2.2.1.6**: \[Frontend\] Implement the UI and interaction for the pagination component.  
    * **Task 2.2.1.7**: \[Frontend\] Add a user-friendly "No products found" message UI.  
  * **Story 2.2.2: View Product Details**  
    * **Description**: As a **Visitor or User**, I want to **click a product in a list to view its details page, including all images, description, price, and seller information** so that **I can decide whether to buy it.**  
    * **Task 2.2.2.1**: \[Backend\] Develop an API to fetch the detailed information for a single product.  
    * **Task 2.2.2.2**: \[Backend\] Handle logic in the API for when a product does not exist or has been delisted.  
    * **Task 2.2.2.3**: \[Frontend\] Create the product details page UI, including an image carousel, product info section, and seller info link.  
    * **Task 2.2.2.4**: \[Frontend\] Dynamically show/hide/disable the "Add to Cart" and "Buy Now" buttons based on product status (ONSALE, LOCKED, SOLD) and user login state.  
    * **Task 2.2.2.5**: \[Frontend\] Integrate the review list with pagination.

#### **Epic 3: Core Transaction Flow**

* **Feature 3.1: Shopping Cart Management (UC-06)**  
  * **Story 3.1.1: Add Product to Cart**  
    * **Description**: As a **User**, I want to **click the "Add to Cart" button on a product page** so that **I can group multiple items I want to buy for later checkout.**  
    * **Task 3.1.1.1**: \[Backend\] Design the shopping cart data model (can be based on Redis or a database table).  
    * **Task 3.1.1.2**: \[Backend\] Develop the "Add to Cart" API, including validation for product status and preventing users from buying their own items.  
    * **Task 3.1.1.3**: \[Frontend\] Implement the click event for the "Add to Cart" button to call the API and show a success notification.  
  * **Story 3.1.2: View and Manage Shopping Cart**  
    * **Description**: As a **User**, I want to **go to my shopping cart page to see all items, remove items, and select items for checkout** so that **I can confirm my purchase list.**  
    * **Task 3.1.2.1**: \[Backend\] Develop an API to fetch the shopping cart list.  
    * **Task 3.1.2.2**: \[Backend\] Develop an API to remove an item from the cart.  
    * **Task 3.1.2.3**: \[Frontend\] Create the shopping cart page UI, displaying items in a list format.  
    * **Task 3.1.2.4**: \[Frontend\] Implement interactive features like item selection, select all, and removal.  
    * **Task 3.1.2.5**: \[Frontend\] Implement logic to display a notification and disable selection for items that have become unavailable.  
* **Feature 3.2: Order Creation & Payment (UC-07)**  
  * **Story 3.2.1: Create an Order**  
    * **Description**: As a **User**, I want to **proceed to an order confirmation page after clicking "Checkout" and submit the order** so that **I can formally lock in the items I want to purchase.**  
    * **Task 3.2.1.1**: \[Backend\] Design the Order database schema, including all product snapshot fields.  
    * **Task 3.2.1.2**: \[Backend\] Develop the order creation API, which must perform real-time validation of all product statuses before execution.  
    * **Task 3.2.1.3**: \[Backend\] Implement the logic to persist product snapshot information (title, price, etc.) upon order creation.  
    * **Task 3.2.1.4**: \[Backend\] Implement logic to set the product status to LOCKED and the order status to AWAITING\_PAYMENT after order creation.  
    * **Task 3.2.1.5**: \[Frontend\] Create the order confirmation page UI, displaying items to be purchased and the total price.  
    * **Task 3.2.1.6**: \[Frontend\] Implement the API call for the "Submit Order" button.  
  * **Story 3.2.2: Simulated Payment**  
    * **Description**: As a **User**, I want to **be directed to a simulated payment page after submitting my order and click a button to complete the payment** so that **I can finalize the purchase process.**  
    * **Task 3.2.2.1**: \[Backend\] Develop a simulated payment callback API to update the order status from AWAITING\_PAYMENT to AWAITING\_SHIPPING.  
    * **Task 3.2.2.2**: \[Backend\] Trigger notifications to the buyer and seller upon successful payment.  
    * **Task 3.2.2.3**: \[Frontend\] Create a simple simulated payment page with a "Confirm Payment" button.  
* **Feature 3.3: Order Lifecycle Management (UC-08)**  
  * **Story 3.3.1: Seller Ships Order**  
    * **Description**: As a **Seller**, I want to **perform a "ship" action on "Awaiting Shipping" orders in my "Sold Orders" list** so that **I can inform the buyer that the item is on its way.**  
    * **Task 3.3.1.1**: \[Backend\] Develop the seller's "Ship Order" API to update the order status to SHIPPED.  
    * **Task 3.3.1.2**: \[Backend\] Implement the logic to send a notification to the buyer after shipping.  
    * **Task 3.3.1.3**: \[Frontend\] Provide a "Ship" button for orders with AWAITING\_SHIPPING status in the seller's order management interface.  
  * **Story 3.3.2: Buyer Confirms Receipt**  
    * **Description**: As a **Buyer**, I want to **perform a "Confirm Receipt" action on "Shipped" orders in my "My Purchases" list** so that **I can complete the transaction.**  
    * **Task 3.3.2.1**: \[Backend\] Develop the buyer's "Confirm Receipt" API to update the order status to COMPLETED and the corresponding product status to SOLD.  
    * **Task 3.3.2.2**: \[Backend\] Implement the logic to send a notification to the seller after receipt confirmation.  
    * **Task 3.3.2.3**: \[Frontend\] Provide a "Confirm Receipt" button for orders with SHIPPED status in the buyer's order management interface.  
* **Feature 3.4: Reviews & After-Sales (UC-09, UC-10, UC-18)**  
  * **Story 3.4.1: Buyer Writes a Review**  
    * **Description**: As a **Buyer**, I want to **leave a star rating and a text comment for a completed transaction** so that **I can share my shopping experience.**  
    * **Task 3.4.1.1**: \[Backend\] Design the Review database schema.  
    * **Task 3.4.1.2**: \[Backend\] Develop the submit review API, validating that the order status is COMPLETED and has not yet been reviewed.  
    * **Task 3.4.1.3**: \[Backend\] Implement logic to update the seller's average rating after a new review is submitted.  
    * **Task 3.4.1.4**: \[Frontend\] Provide a "Review" entry point next to completed orders.  
    * **Task 3.4.1.5**: \[Frontend\] Create the review submission form (star rating \+ text area).  
  * **Story 3.4.2: Buyer Requests a Return**  
    * **Description**: As a **Buyer**, I want to **request a return for a completed order within a specified timeframe** so that **I can resolve issues with unsatisfactory products.**  
    * **Task 3.4.2.1**: \[Backend\] Develop the return request API, validating order status and time window, and updating the order status to RETURN\_REQUESTED.  
    * **Task 3.4.2.2**: \[Frontend\] Provide an "Apply for After-Sales" entry point next to eligible completed orders.  
  * **Story 3.4.3: Seller Processes Return Request**  
    * **Description**: As a **Seller**, I want to **process a buyer's return request by choosing to "Approve" or "Deny" it** so that **I can manage after-sales issues.**  
    * **Task 3.4.3.1**: \[Backend\] Develop the API to process return requests, implementing logic for approval (status becomes RETURNED, simulated refund is processed) and denial (status reverts to COMPLETED, reason is recorded).  
    * **Task 3.4.3.2**: \[Backend\] Implement logic to notify the buyer of the outcome.  
    * **Task 3.4.3.3**: \[Frontend\] Provide a "Process" entry point and corresponding UI for orders with RETURN\_REQUESTED status in the seller's order management interface.

#### **Epic 4: Social & Notifications**

* **Feature 4.1: In-App Messaging (UC-11)**  
  * **Story 4.1.1: Inquire About a Product**  
    * **Description**: As a **User**, I want to **click "Contact Seller" on a product page to open a chat window and ask questions** so that **I can learn more details before buying.**  
    * **Task 4.1.1.1**: \[Backend\] Design the database schemas for messages and conversations.  
    * **Task 4.1.1.2**: \[Backend\] Implement a real-time messaging service using WebSockets or long polling.  
    * **Task 4.1.1.3**: \[Backend\] Develop the send message API, binding the conversation to the product ID and the buyer/seller IDs.  
    * **Task 4.1.1.4**: \[Frontend\] Implement the "Contact Seller" button on the product details page.  
    * **Task 4.1.1.5**: \[Frontend\] Develop the chat window component, including message history display, input box, and send button.  
* **Feature 4.2: Follows & Activity Feed (UC-12)**  
  * **Story 4.2.1: Follow Sellers and Receive Updates**  
    * **Description**: As a **User**, I want to **follow sellers I like and see a feed of their new product listings on an "Activity" page** so that **I can stay updated on their latest items.**  
    * **Task 4.2.1.1**: \[Backend\] Design the Follows table for user relationships.  
    * **Task 4.2.1.2**: \[Backend\] Develop a background job or trigger that generates an activity feed item for all of a seller's followers when a product is first approved (PENDING\_REVIEW \-\> ONSALE).  
    * **Task 4.2.1.3**: \[Backend\] Develop an API to fetch the "Activity Feed".  
    * **Task 4.2.1.4**: \[Frontend\] Create the "Activity Feed" page UI.  
* **Feature 4.3: Unified Notification Center (UC-13)**  
  * **Story 4.3.1: Receive and View System Notifications**  
    * **Description**: As a **User**, I want to **receive alerts for all important events (like order status changes, new messages) in a centralized notification center with an unread badge** so that **I don't miss any important information.**  
    * **Task 4.3.1.1**: \[Backend\] Design the Notification database schema.  
    * **Task 4.3.1.2**: \[Backend\] Embed logic to create notifications at all key business events (order status change, new message, review result, etc.).  
    * **Task 4.3.1.3**: \[Backend\] Develop APIs to fetch the notification list and the unread count.  
    * **Task 4.3.1.4**: \[Frontend\] Implement the notification center icon with an unread badge in the global navigation bar.  
    * **Task 4.3.1.5**: \[Frontend\] Create the notification list popover or page, displaying summaries and links.

#### **Epic 5: Admin Backend System**

* **Feature 5.1: Admin Core Functionality (UC-14, UC-15)**  
  * **Story 5.1.1: Secure Admin Login**  
    * **Description**: As an **Administrator**, I want to **log in to the management system via a dedicated URL with my admin credentials** so that **I can perform my administrative duties.**  
    * **Task 5.1.1.1**: \[Backend\] Implement a separate authentication logic for admins, including role/permission checks.  
    * **Task 5.1.1.2**: \[Backend\] Implement a secondary password confirmation mechanism for high-risk operations.  
    * **Task 5.1.1.3**: \[Frontend\] Create the admin login page.  
  * **Story 5.1.2: View Data Dashboard**  
    * **Description**: As an **Administrator**, I want to **see a dashboard with core KPIs and charts upon login** so that **I can quickly understand the overall health of the platform.**  
    * **Task 5.1.2.1**: \[Backend\] Develop APIs to aggregate statistical data for the dashboard.  
    * **Task 5.1.2.2**: \[Frontend\] Design and implement the dashboard page, using a charting library (e.g., ECharts, Recharts) to visualize data.  
* **Feature 5.2: Content & User Management (UC-16, UC-17)**  
  * **Story 5.2.1: Review and Manage Products**  
    * **Description**: As an **Administrator**, I want to **review pending products (approve/reject) and be able to delist or delete non-compliant products** so that **I can maintain the quality of platform content.**  
    * **Task 5.2.1.1**: \[Backend\] Develop product management APIs (approve, reject, delist, delete).  
    * **Task 5.2.1.2**: \[Frontend\] Create the product review and management list pages.  
  * **Story 5.2.2: Manage User Accounts**  
    * **Description**: As an **Administrator**, I want to **search for users and apply "Mute" or "Ban" actions to their accounts** so that **I can manage the platform's user community.**  
    * **Task 5.2.2.1**: \[Backend\] Develop APIs for user search and status modification (MUTE/BAN).  
    * **Task 5.2.2.2**: \[Frontend\] Create the user management page.  
* **Feature 5.3: Auditing & Traceability (UC-19)**  
  * **Story 5.3.1: View Audit Logs**  
    * **Description**: As an **Administrator**, I want to **view a log of all sensitive operations and be able to filter and search them** so that **I can perform security audits and trace issues.**  
    * **Task 5.3.1.1**: \[Backend\] Design the AuditLog database schema, including all required fields (NFR-SEC-03).  
    * **Task 5.3.1.2**: \[Backend\] Embed audit logging code into the business logic of all sensitive admin operations (UC-16, UC-17, etc.).  
    * **Task 5.3.1.3**: \[Backend\] Develop the audit log query API with filtering and search capabilities.  
    * **Task 5.3.1.4**: \[Frontend\] Create the audit log viewer page.

#### **Epic 6: System-Level Requirements & Infrastructure**

* **Feature 6.1: System Performance Optimization**  
  * **Story 6.1.1: Improve Key Page Load Speed**  
    * **Description**: As the **Development Team**, we need to **optimize the frontend performance of key pages to ensure the LCP is under 3 seconds on a "Fast 3G" network (NFR-PERF-02)** so that **we can improve the user experience.**  
    * **Task 6.1.1.1**: \[DevOps/Arch\] Set up a CDN to serve static assets (images, JS, CSS).  
    * **Task 6.1.1.2**: \[Frontend\] Compress and optimize all uploaded images (e.g., convert to WEBP).  
    * **Task 6.1.1.3**: \[Frontend\] Implement code splitting and lazy loading strategies.  
  * **Story 6.1.2: Ensure System Concurrency**  
    * **Description**: As the **Operations Team**, we need to **conduct stress tests to ensure the system can handle 500 concurrent users with a P95 response time under 500ms on reference hardware (NFR-PERF-01)** so that **we can guarantee system stability.**  
    * **Task 6.1.2.1**: \[QA\] Write stress testing scripts (e.g., using JMeter/k6) to simulate the standard transaction mix.  
    * **Task 6.1.2.2**: \[Backend\] Identify and optimize slow database queries.  
    * **Task 6.1.2.3**: \[Backend\] Introduce a caching layer (e.g., Redis) for hot data like homepage products and categories.  
* **Feature 6.2: System Security Hardening**  
  * **Story 6.2.1: Implement Application Security Best Practices**  
    * **Description**: As the **Security Team**, we need to **enforce site-wide HTTPS (NFR-SEC-01), use parameterized queries to prevent SQL injection (NFR-SEC-04), and sanitize UGC to prevent XSS (NFR-SEC-05)** so that **we can protect platform and user data.**  
    * **Task 6.2.1.1**: \[DevOps\] Configure the web server or gateway to enforce HTTP to HTTPS redirection.  
    * **Task 6.2.1.2**: \[Backend\] Conduct a full code review to ensure all database interactions use an ORM or prepared statements.  
    * **Task 6.2.1.3**: \[Frontend\] Integrate and configure a library like DOMPurify to sanitize all UGC before rendering.  
* **Feature 6.3: Reliability & Maintainability**  
  * **Story 6.3.1: Establish Comprehensive Logging and Monitoring**  
    * **Description**: As the **Dev & Ops Teams**, we need to **implement structured logging (NFR-MAIN-01), provide a health check endpoint (NFR-MAIN-02), and integrate an error tracking service (NFR-MAIN-03)** so that **we can quickly identify and resolve production issues.**  
    * **Task 6.3.1.1**: \[Backend\] Introduce a logging framework (e.g., Logback, Serilog) and configure JSON output format.  
    * **Task 6.3.1.2**: \[Backend\] Implement a /health endpoint that returns { "status": "UP" }.  
    * **Task 6.3.1.3**: \[Frontend/Backend\] Integrate the Sentry SDK (or similar) and configure reporting.  
    * **Task 6.3.1.4**: \[DevOps\] Set up a log aggregation tool (e.g., ELK Stack, Grafana Loki) and configure log retention policies (NFR-MAIN-04).