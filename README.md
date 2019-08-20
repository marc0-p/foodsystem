# foodsystem

This system simulates a kitchen which accepts, and processes food/beverage orders.
Various business metrics are computed, as well as metrics for analyzing the performance of the kitchen configuration.

## Getting Started

This project was built using Maven.
https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html

To build the project run:
mvn clean install

The above command will build an executable JAR and execute any unit tests.
The JAR should be saved as: target/foodsystem-1.0-SNAPSHOT-manual.jar

The latest version of the JAR is already included in the above location, if you don't want to rebuild the project.

### Running the Application

The main application is called OrderProcessor.  This app will process a set of orders passed as a JSON file.
The command to run the app is (from the foodsystem repo root dir):
java -jar target/foodsystem-1.0-SNAPSHOT-manual.jar
args:
- -kmc number of concurrent items allowed to process
- -kn kitchen name.. use testKitchen, since it is the only kitchen configured
- -ip input path to orders JSON file
- -op output path for stats

An example command is:
java -jar target/foodsystem-1.0-SNAPSHOT-manual.jar -kmc 20 -kn testKitchen -ip /Users/mp/orderdata/orders.json -op /Users/mp/orderdata/output/stats

### Outputs

Running the application will produce the following output.
- An HTML page with descriptive stats, charts and tables.
- Charts as PNG files
- Tables as raw CSV files

### Included Stats

- Number of orders completed
- Number of orders rejected (usually this is due to zero items, or missing timestamp).
- Total Revenue
- Distribution of Orders by price
- Distribution of Orders by time in pending state
- Revenue by Service
- Revenue by Item

## System design

### Logical Data Model

- Kitchen: An entity representing a physical kitchen configuration.
- Menu: A list of items which can be prepared, and belong to a certain subset. Also attributes which describe the subset.
- MenuItem: An item which can be ordered and/or prepared by a Kitchen (includes attributes such as cook time).
- Order: Represents a food order, which has a list of items, and other attributes.
- OrderItem: An item associated with an order, and appropriate attributes (e.g. price paid).
- OrderProcessingStrategy: An enum describing how orders should be processed (e.g. First come, first serve)
- OrderState: State of the order, e.g. PROCESSING, COMPLETE, etc.
- ItemState: State of the OrderItem e.g. PROCESSING, COMPLETE, etc.

- A Kitchen has 1:n Menus
- A Menu has 1:n MenuItems
- An Order has 1:n OrderItems

### Store

OrderStore: This is a data access layer for orders and stats, and is the core of the system.  The interface is designed,
            so that the OrderStore can be implemented with any storage technology. (e.g. Cassandra, mySql, etc.).

OrderInMemoryStore: This implementation of an OrderStore is purely in-memory.  It is not intended to scale.  This is a proof-of-concept.
                    For an enterprise system, there would be an implementation of OrderStore backed by an enterprise-level storage solution.

### Design for Stats

The idea for this design, is that the OrderStore has all of the information needed to generate any stats required.
(E.g. entity state change history).  Furthermore, generally stats should be updated rather than computed.

But since the API for retrieving the stats is part of the store, is doesn't make a difference to the caller.

For an enterprise scale solution, the API would need additional features, such as pagination, which are not included now.

### Application

OrderProcessor: This is the main application.

### Builders

KitchenBuilder: Creates a valid kitchen.

### Charts

ChartUtils: This is a collection of static methods for building charts, tables, and the stats page.

### DTO

These classes are used for deserializing JSON orders, and items into the objects described in the Logical Data Model

### Indexing

KitchenMenuItemIndexes: Creates indexes such as cookTimeByMenuItemName, for fast lookup of cook times across multiple menus, for a Kitchen.
