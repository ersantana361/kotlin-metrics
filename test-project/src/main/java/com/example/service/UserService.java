package com.example.service;

import com.example.User;
import java.util.List;
import java.util.ArrayList;

public class UserService {
    private final List<User> users = new ArrayList<>();
    
    public User createUser(String name, String email) {
        if (name == null || email == null) {
            throw new IllegalArgumentException("Name and email cannot be null");
        }
        
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setId(generateId());
        
        users.add(user);
        return user;
    }
    
    public User findById(Long id) {
        for (User user : users) {
            if (user.getId().equals(id)) {
                return user;
            }
        }
        return null;
    }
    
    public List<User> findByEmail(String email) {
        List<User> result = new ArrayList<>();
        for (User user : users) {
            if (user.getEmail().equals(email)) {
                result.add(user);
            }
        }
        return result;
    }
    
    public boolean deleteUser(Long id) {
        return users.removeIf(user -> user.getId().equals(id));
    }
    
    public void updateUser(User user) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equals(user.getId())) {
                users.set(i, user);
                return;
            }
        }
        throw new IllegalArgumentException("User not found");
    }
    
    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }
    
    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }
    
    private Long generateId() {
        return (long) (Math.random() * 10000);
    }
    
    public int getUserCount() {
        return users.size();
    }
    
    public void clearUsers() {
        users.clear();
    }
}