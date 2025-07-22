package com.shiwu.util;

import com.shiwu.common.util.PasswordUtil;

public class PasswordTestHelper {
    public static void main(String[] args) {
        String password = "password123";
        String hashedPassword = PasswordUtil.encrypt(password);
        System.out.println("Original: " + password);
        System.out.println("Hashed: " + hashedPassword);
        System.out.println("Matches: " + PasswordUtil.matches(password, hashedPassword));
    }
}
