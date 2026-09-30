package com.example.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class UserService {
    
    private final Map<Long, User> users = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    public UserService() {
        // Initialize with sample data
        users.put(1L, new User(1L, "John Doe", "john@example.com", 30));
        users.put(2L, new User(2L, "Jane Smith", "jane@example.com", 25));
        users.put(3L, new User(3L, "Bob Johnson", "bob@example.com", 35));
        idGenerator.set(4);
    }
    
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }
    
    public User getUserById(@Header("id") String id) {
        try {
            Long userId = Long.parseLong(id);
            return users.get(userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    public User createUser(User user, Exchange exchange) {
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
            return null;
        }
        
        Long newId = idGenerator.getAndIncrement();
        user.setId(newId);
        users.put(newId, user);
        
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 201);
        return user;
    }
    
    public User updateUser(@Header("id") String id, User updatedUser, Exchange exchange) {
        try {
            Long userId = Long.parseLong(id);
            User existingUser = users.get(userId);
            
            if (existingUser == null) {
                exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
                return null;
            }
            
            updatedUser.setId(userId);
            users.put(userId, updatedUser);
            
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
            return updatedUser;
        } catch (NumberFormatException e) {
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
            return null;
        }
    }
    
    public String deleteUser(@Header("id") String id, Exchange exchange) {
        try {
            Long userId = Long.parseLong(id);
            User removedUser = users.remove(userId);
            
            if (removedUser == null) {
                exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
                return "{\"message\":\"User not found\"}";
            }
            
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
            return "{\"message\":\"User deleted successfully\"}";
        } catch (NumberFormatException e) {
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
            return "{\"message\":\"Invalid user ID\"}";
        }
    }
}
