package dao;
import exceptions.UserNotFoundException;
import exceptions.OrderNotFoundException;
import db.DBUtil;
import entity.Product;
import entity.User;

import java.sql.*;
import java.util.*;

public class OrderProcessor implements OrderManagementRepository {

    @Override
    public void createUser(User user) throws Exception {
        Connection conn = DBUtil.getDBConn();
        String query = "INSERT INTO users (userId, username, password, role) VALUES (?, ?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, user.getUserId());
        stmt.setString(2, user.getUsername());
        stmt.setString(3, user.getPassword());
        stmt.setString(4, user.getRole());
        stmt.executeUpdate();
        conn.close();
    }

    @Override
    public void createProduct(User user, Product product) throws Exception {
        if (!"Admin".equalsIgnoreCase(user.getRole())) {
            throw new Exception("Only admin can create products");
        }
        Connection conn = DBUtil.getDBConn();
        String query = "INSERT INTO products (productId, productName, description, price, quantityInStock, type) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, product.getProductId());
        stmt.setString(2, product.getProductName());
        stmt.setString(3, product.getDescription());
        stmt.setDouble(4, product.getPrice());
        stmt.setInt(5, product.getQuantityInStock());
        stmt.setString(6, product.getType());
        stmt.executeUpdate();
        conn.close();
    }

    @Override
    public void createOrder(User user, List<Product> products) throws Exception {
        Connection conn = DBUtil.getDBConn();

        String checkUserQuery = "SELECT * FROM users WHERE userId = ?";
        PreparedStatement checkUserStmt = conn.prepareStatement(checkUserQuery);
        checkUserStmt.setInt(1, user.getUserId());
        ResultSet rs = checkUserStmt.executeQuery();

        if (!rs.next()) {
            createUser(user);
        }

        String orderInsert = "INSERT INTO orders (userId) VALUES (?)";
        PreparedStatement orderStmt = conn.prepareStatement(orderInsert, Statement.RETURN_GENERATED_KEYS);
        orderStmt.setInt(1, user.getUserId());
        orderStmt.executeUpdate();
        ResultSet generatedKeys = orderStmt.getGeneratedKeys();
        int orderId = 0;
        if (generatedKeys.next()) {
            orderId = generatedKeys.getInt(1);
        }

        String orderProductInsert = "INSERT INTO orderproducts (orderId, productId) VALUES (?, ?)";
        for (Product p : products) {
            PreparedStatement orderProductStmt = conn.prepareStatement(orderProductInsert);
            orderProductStmt.setInt(1, orderId);
            orderProductStmt.setInt(2, p.getProductId());
            orderProductStmt.executeUpdate();
        }

        conn.close();
    }

    @Override
    public void cancelOrder(int userId, int orderId) throws UserNotFoundException, OrderNotFoundException, SQLException {
        Connection conn = DBUtil.getDBConn();

        // Check if user exists
        PreparedStatement checkUser = conn.prepareStatement("SELECT * FROM users WHERE userId = ?");
        checkUser.setInt(1, userId);
        ResultSet rsUser = checkUser.executeQuery();
        if (!rsUser.next()) {
            throw new UserNotFoundException("User with ID " + userId + " not found.");
        }

        // Check if order exists and belongs to the user
        PreparedStatement checkOrder = conn.prepareStatement("SELECT * FROM orders WHERE orderId = ? AND userId = ?");
        checkOrder.setInt(1, orderId);
        checkOrder.setInt(2, userId);
        ResultSet rsOrder = checkOrder.executeQuery();
        if (!rsOrder.next()) {
            throw new OrderNotFoundException("Order with ID " + orderId + " for User ID " + userId + " not found.");
        }

        // Cancel the order (update status)
        PreparedStatement cancelOrder = conn.prepareStatement("UPDATE orders SET status = 'CANCELLED' WHERE orderId = ?");
        cancelOrder.setInt(1, orderId);
        int rows = cancelOrder.executeUpdate();
        if (rows > 0) {
            System.out.println("Order cancelled successfully.");
        } else {
            System.out.println("Failed to cancel the order.");
        }

        conn.close();
    }


    @Override
    public List<Product> getAllProducts() throws Exception {
        List<Product> products = new ArrayList<>();
        Connection conn = DBUtil.getDBConn();
        String query = "SELECT * FROM products";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        while (rs.next()) {
            Product p = new Product(
                rs.getInt("productId"),
                rs.getString("productName"),
                rs.getString("description"),
                rs.getDouble("price"),
                rs.getInt("quantityInStock"),
                rs.getString("type")
            );
            products.add(p);
        }
        conn.close();
        return products;
    }

   
    @Override
    public List<Product> getOrderByUser(User user) throws Exception {
        List<Product> products = new ArrayList<>();
        Connection conn = DBUtil.getDBConn();
        String query = "SELECT p.* FROM products p " +
                       "JOIN orderproducts op ON p.productId = op.productId " +
                       "JOIN orders o ON op.orderId = o.orderId " +
                       "WHERE o.userId = ? AND o.status != 'CANCELLED'";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, user.getUserId());
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            Product p = new Product(
                rs.getInt("productId"),
                rs.getString("productName"),
                rs.getString("description"),
                rs.getDouble("price"),
                rs.getInt("quantityInStock"),
                rs.getString("type")
            );
            products.add(p);
        }
        conn.close();
        return products;
    }


  
    @Override
    public double getTotalAmountSpentByUser(User user) throws Exception {
        double total = 0;
        Connection conn = DBUtil.getDBConn();

        String query = "SELECT SUM(p.price * op.quantity) AS totalSpent " +
                       "FROM products p " +
                       "JOIN orderproducts op ON p.productId = op.productId " +
                       "JOIN orders o ON op.orderId = o.orderId " +
                       "WHERE o.userId = ? AND o.status != 'CANCELLED'";

        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, user.getUserId());
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            total = rs.getDouble("totalSpent");
        }

        conn.close();
        return total;
    }
}