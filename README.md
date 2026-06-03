**Cache Flow – Personal Budget Management Application**
**Introduction**
Cache Flow is a mobile application developed to assist users in managing their personal finances more effectively. The app provides tools for recording expenses, organising transactions into categories, setting spending targets, and analysing financial activity through reports and visual representations.

The application is primarily aimed at students and young adults who require a straightforward and accessible solution for monitoring their finances and developing responsible spending habits.

**Purpose of the Application**
Many individuals struggle to keep track of their finances, particularly when balancing studies, work, and daily expenses. Cache Flow was created to address this challenge by providing users with a centralised platform where they can monitor spending, establish financial goals, and gain insights into their financial behaviour.

The main objectives of the application are to:

- Record and manage expenses efficiently
- Categorise transactions for better organisation
- Support budgeting and financial planning
- Provide meaningful spending insights through reports and graphs
- Encourage users to develop healthier financial habits

**Application Features
User Authentication**
The application includes a login system that requires users to enter a valid username and password before accessing the system. This helps ensure that personal financial information remains secure.

**Expense Management**
Users can create and store expense entries by providing details such as:
- Date
- Time
- Description
- Amount spent
- Expense category
Users also have the option of attaching a photograph, such as a receipt, to support their expense records.

**Category Management**
The system allows users to create their own spending categories. Examples include food, transport, entertainment, and utilities.

These categories help users organise transactions and provide more meaningful financial reports.

**Spending Analysis Graph**
The application includes graphical reports that display spending across different categories for a selected period.
The graph also includes indicators for:
- Minimum spending goal
- Maximum spending goal
This allows users to compare their spending against their predefined financial targets.

**Budget Progress Monitoring**
A visual budgeting component is included to show how successfully a user is staying within their spending limits.

The feature provides clear feedback using visual indicators to help users determine whether they are:

- Within budget
- Approaching their limit
- Exceeding their target spending amount

**Interest Calculator**
An interest calculator is included to assist users with basic financial planning.

The calculator enables users to estimate how money may grow over time based on an interest rate and investment period. This feature can be useful for savings planning and understanding financial growth.

**Photo Scanner**
The photo scanner allows users to capture or upload images of receipts and associate them with specific expense entries.

This feature improves record keeping and provides supporting documentation for expenses entered into the system.

**Online Database Integration**
Unlike the prototype version that relied on local storage, the final application stores information in a cloud-based database using Firebase Firestore.

This allows data to remain available across sessions while providing reliable and secure storage.

**Technologies and Tools**
The following technologies were used during development:
- Android Studio
- Java/Kotlin
- XML
- Firebase Firestore
- MPAndroidChart
- Git
- GitHub
- GitHub Actions
These technologies were selected to support mobile development, cloud data storage, version control, and automated project management.

**Database Design**
The application database consists of several collections that store different types of information.

**User Information**
Stores account-related information, including:
- User ID
- Username
- Password

**Expense Information**
Stores transaction details, including:
- Expense ID
- Amount
- Description
- Date
- Time
- Category
- Image reference

**Category Information**
Stores category records, including:
- Category ID
- Category Name

**Budget Goals**
Stores spending targets, including:
- Minimum budget goal
- Maximum budget goal

**Application Screens**
The following screenshots demonstrate the main functionality of the application:
Login Screen
<img width="387" height="680" alt="e3fd377b-91d3-45ec-9e6c-be0644bd378a" src="https://github.com/user-attachments/assets/332bca79-a6e4-48c9-aa54-407429bf44a1" />

Dashboard Screen
<img width="269" height="468" alt="image" src="https://github.com/user-attachments/assets/2314e5e2-e637-4ba6-b420-ce5a1ab0a047" />

Add Expense Screen
<img width="271" height="472" alt="image" src="https://github.com/user-attachments/assets/d560f16a-e849-4818-99bd-80694d0df41b" />

Category Management Screen
<img width="271" height="472" alt="image" src="https://github.com/user-attachments/assets/2e32ebfe-831d-4e1f-bc6e-26b8d40658e4" />

Spending Analysis Screen
<img width="268" height="470" alt="image" src="https://github.com/user-attachments/assets/8bd2b8a7-ed6d-456a-886e-aa809106895a" />

Budget Progress Screen
<img width="268" height="470" alt="image" src="https://github.com/user-attachments/assets/2473c9cc-44a9-4ba1-9974-f3d96046cd54" />

**Navigation Structure**
The application follows the navigation path shown below:

**Login → Dashboard → Add Expense → View Expenses → Reports → Graph Analysis → Budget Goals**

This structure was designed to provide a logical and user-friendly workflow.

**Use of GitHub**
GitHub was used throughout the development process as a version control platform.

Its use provided several benefits, including:

- Maintaining a history of project changes
- Tracking feature development
- Managing code updates
- Providing a backup of project files
Regular commits were made to document progress and support project management.

**GitHub Actions**
GitHub Actions was incorporated to automate aspects of the development process.

The workflow assists by:

- Automatically building the project
- Identifying build errors
- Verifying that changes do not negatively impact the application
This contributes to maintaining project quality and stability.

**User Interface and Design Considerations**
Several design principles were considered during development to ensure a positive user experience.

These include:

- Simplicity and ease of use
- Consistent visual design
- Clear navigation between screens
- Readable layouts and controls
- Effective use of graphs and visual feedback
The overall goal was to create an application that is both functional and intuitive.

**Inspiration and Research**
The design of Cache Flow was influenced by research conducted on popular budgeting applications, including:
- Mint
- Spendee
- Budge: My Budget Planner
Features such as spending analytics, category-based budgeting, and financial goal tracking were inspired by these applications and adapted for this project.

**Potential Future Enhancements**
- Future versions of the application could include:
- Biometric authentication
- AI-powered spending recommendations
- Bank account integration
- Exporting reports to PDF format
- Enhanced visual themes and dark mode support
These improvements would further enhance the application's functionality and user experience.

**Summary**
Cache Flow provides a complete budgeting solution that combines expense tracking, category management, spending analysis, and financial goal monitoring within a single mobile application.

The integration of cloud storage, graphical reporting, and custom features such as the interest calculator and receipt scanner contributes to a more comprehensive and practical financial management tool. The project demonstrates both technical implementation and user-centred design principles while addressing real-world budgeting challenges.

**Developer Information**
**Developers:** Braedy Dillon, Joel Napier & Vianka Mahabeer
**Project:** Cache Flow – Personal Budget Management Application
**Module:** PROG7313
**Year:** 2026
