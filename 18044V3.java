import java.sql.*;
import java.util.*;

class Product 
{
    private int id;
    private String name;
    private int price;

    public Product(int id, String name, int price) 
    {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public int getId() 
    {
        return id;
    }

    public String getName() 
    {
        return name;
    }

    public int getPrice() 
    {
        return price;
    }

    public String toString() 
    {
        return id + ". " + name + " (Rupees " + price + ")";
    }
}

public class StationeryShopJDBC1 
{
    private static final String DB_URL = "jdbc:mysql://localhost:3306/syitbd";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "";

    public static void main(String[] args) 
    {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
             Scanner scanner = new Scanner(System.in)) 
        {
            System.out.println("Connected to database successfully!");

            displayProducts(conn);

            // Prompt and validate customer name
            String customerName = "";
            while (true) 
            {
                System.out.print("Enter customer name: ");
                customerName = scanner.nextLine();
                if (isValidCustomerName(customerName)) 
                {
                    break;
                } 
                else 
                {
                    System.out.println("Invalid name! Please enter a name using only alphabets and spaces.");
                }
            }

            int customerId = addCustomer(conn, customerName);

            boolean finished = false;
            List<OrderItem> orderItems = new ArrayList<>();

            while (!finished) 
            {
                System.out.print("Select a product by ID (0 to finish, -1 to remove an item, -2 to reduce quantity): ");
                int productId = scanner.nextInt();

                if (productId == 0) 
                {
                    finished = true;
                } 
                else if (productId == -1) 
                {
                    removeItem(orderItems, scanner);
                } 
                else if (productId == -2) 
                {
                    reduceQuantity(orderItems, scanner);
                } 
                else 
                {
                    if (!isValidProductId(conn, productId)) 
                    {
                        System.out.println("Invalid product ID. Please try again.");
                        continue;
                    }

                    System.out.print("Enter quantity (must be greater than zero): ");
                    int quantity = scanner.nextInt();

                    if (quantity <= 0) 
                    {
                        System.out.println("Invalid quantity. Quantity must be greater than zero.");
                        continue;
                    }

                    Product product = getProductById(conn, productId);
                    if (product != null) 
                    {
                        orderItems.add(new OrderItem(product, quantity));
                        System.out.println("Added " + quantity + " x " + product.getName() + " to your order.");
                    }
                }
            }

            for (OrderItem item : orderItems) 
            {
                addOrder(conn, customerId, item.getProduct().getId(), item.getQuantity());
            }

            printOrderSummary(conn, customerId);
        } 
        catch (SQLException e) 
        {
            e.printStackTrace();
        }
    }

    private static boolean isValidCustomerName(String name) 
    {
        return name.matches("^[a-zA-Z\\s]+$"); // Allows alphabets and spaces only
    }

    private static boolean isValidProductId(Connection conn, int productId) throws SQLException 
    {
        String query = "SELECT COUNT(*) FROM products WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) 
        {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) 
            {
                if (rs.next() && rs.getInt(1) > 0) 
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static void reduceQuantity(List<OrderItem> orderItems, Scanner scanner) 
    {
        if (orderItems.isEmpty()) 
        {
            System.out.println("No items in the order to reduce quantity.");
            return;
        }

        System.out.println("Current items in your order:");
        for (int i = 0; i < orderItems.size(); i++) 
        {
            System.out.println((i + 1) + ". " + orderItems.get(i));
        }

        System.out.print("Select the item number to reduce quantity: ");
        int itemNumber = scanner.nextInt();
        if (itemNumber > 0 && itemNumber <= orderItems.size()) 
        {
            OrderItem item = orderItems.get(itemNumber - 1);
            System.out.print("Enter quantity to reduce: ");
            int reduceQuantity = scanner.nextInt();
            if (reduceQuantity > 0 && reduceQuantity <= item.getQuantity()) 
            {
                item.reduceQuantity(reduceQuantity);
                if (item.getQuantity() == 0) 
                {
                    orderItems.remove(itemNumber - 1);
                }
                System.out.println("Reduced quantity successfully.");
            } 
            else 
            {
                System.out.println("Invalid quantity to reduce.");
            }
        } 
        else 
        {
            System.out.println("Invalid item number.");
        }
    }

    private static void displayProducts(Connection conn) throws SQLException 
    {
        System.out.println("Available products:");
        String query = "SELECT * FROM products";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) 
        {
            while (rs.next()) 
            {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int price = rs.getInt("price");
                System.out.println(new Product(id, name, price));
            }
        }
    }

    private static int addCustomer(Connection conn, String name) throws SQLException 
    {
        String query = "INSERT INTO customers (name) VALUES (?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) 
        {
            pstmt.setString(1, name);
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) 
            {
                if (keys.next()) 
                {
                    System.out.println("Customer " + name + " inserted successfully.");
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to retrieve customer ID.");
    }

    private static void addOrder(Connection conn, int customerId, int productId, int quantity) throws SQLException 
    {
        String query = "INSERT INTO orders (customer_id, product_id, quantity) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) 
        {
            pstmt.setInt(1, customerId);
            pstmt.setInt(2, productId);
            pstmt.setInt(3, quantity);
            pstmt.executeUpdate();
        }
    }

    private static Product getProductById(Connection conn, int productId) throws SQLException 
    {
        String query = "SELECT * FROM products WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) 
        {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) 
            {
                if (rs.next()) 
                {
                    return new Product(rs.getInt("id"), rs.getString("name"), rs.getInt("price"));
                }
            }
        }
        return null;
    }

    private static void printOrderSummary(Connection conn, int customerId) throws SQLException 
    {
        String query = "SELECT c.name AS customer_name, p.name AS product_name, o.quantity, (p.price * o.quantity) AS total_price " +
                "FROM orders o " +
                "JOIN customers c ON o.customer_id = c.id " +
                "JOIN products p ON o.product_id = p.id " +
                "WHERE o.customer_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) 
        {
            pstmt.setInt(1, customerId);
            try (ResultSet rs = pstmt.executeQuery()) 
            {
                System.out.println("Order Summary:");
                int totalAmount = 0;

                while (rs.next()) 
                {
                    String customerName = rs.getString("customer_name");
                    String productName = rs.getString("product_name");
                    int quantity = rs.getInt("quantity");
                    int totalPrice = rs.getInt("total_price");
                    totalAmount += totalPrice;

                    System.out.println(productName + " (x" + quantity + "): Rupees " + totalPrice);
                }

                System.out.println("Total Amount: Rupees " + totalAmount);
            }
        }
    }

    private static void removeItem(List<OrderItem> orderItems, Scanner scanner) 
    {
        if (orderItems.isEmpty()) 
        {
            System.out.println("No items in the order to remove.");
            return;
        }

        System.out.println("Current items in your order:");
        for (int i = 0; i < orderItems.size(); i++) 
        {
            System.out.println((i + 1) + ". " + orderItems.get(i));
        }

        System.out.print("Select the item number to remove: ");
        int itemNumber = scanner.nextInt();
        if (itemNumber > 0 && itemNumber <= orderItems.size()) 
        {
            orderItems.remove(itemNumber - 1);
            System.out.println("Item removed from the order.");
        } 
        else 
        {
            System.out.println("Invalid item number.");
        }
    }
}

class OrderItem 
{
    private Product product;
    private int quantity;

    public OrderItem(Product product, int quantity) 
    {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() 
    {
        return product;
    }

    public int getQuantity() 
    {
        return quantity;
    }

    public void reduceQuantity(int amount) 
    {
        this.quantity -= amount;
    }

    public int getTotalPriceInINR() 
    {
        return product.getPrice() * quantity;
    }

    public String toString() 
    {
        return product.getName() + " (x" + quantity + "): Rupees " + getTotalPriceInINR();
    }
}
