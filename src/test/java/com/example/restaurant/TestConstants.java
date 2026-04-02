package com.example.restaurant;

public final class TestConstants {

    private TestConstants() {
        throw new IllegalStateException("Utility class");
    }

    // SYSTEM, AUDIT & LOCALIZATION
    public static final String FAKE_ACTION = "TEST_ACTION";
    public static final String FAKE_IP = "192.168.1.100";
    public static final String FAKE_METHOD_SIGNATURE = "void com.example.SomeService.doSomething()";
    public static final String LANG_EN = "en";
    public static final String LANG_PL = "pl";

    // USERS, AUTH & ROLES
    public static final String ANONYMOUS_USER = "anonymousUser";
    public static final String FAKE_DIFF_PASSWORD = "fake-diff-password";
    public static final String FAKE_EMAIL = "test@test.pl";
    public static final String FAKE_HASH = "fake-hash";
    public static final String FAKE_PASSWORD = "fake-password";
    public static final String FAKE_ROLE = "testrole";
    public static final String FAKE_USERNAME = "testusername";
    public static final String NON_EXISTENT_EMAIL = "nonexistent@test.pl";
    public static final String ROLE_CLIENT = "ROLE_CLIENT";
    public static final String ROLE_MANAGER = "ROLE_MANAGER";
    public static final String ROLE_WAITER = "ROLE_WAITER";
    public static final String VALID_PASSWORD = "NewPass123!";

    // ENTITY TOKENS (IDENTIFIERS)
    public static final String FAKE_ACTION_TOKEN = "fake-action-token";
    public static final String FAKE_DISH_TOKEN = "fake-dish-token";
    public static final String FAKE_ORDER_TOKEN = "fake-order-token";
    public static final String FAKE_REPORT_TOKEN = "fake-report-token";
    public static final String FAKE_RESERVATION_TOKEN = "fake-reservation-token";
    public static final String FAKE_TABLE_TOKEN = "fake-table-token";
    public static final String FAKE_USER_TOKEN = "fake-token";
    public static final String FAKE_VERIFICATION_TOKEN = "fake-verification-token";
    public static final String TOKEN_1 = "TOKEN_1";
    public static final String TOKEN_2 = "TOKEN_2";
    public static final String TOKEN_NON_EXISTENT = "NON_EXISTENT";


    // STATUSES (BANS, ORDERS, REPORTS, TABLES)
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_CLEANING = "CLEANING";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_READY = "READY";

    // DOMAIN DATA (DISHES, INGREDIENTS, ALLERGENS)
    public static final String FAKE_ALLERGEN_EN = "Gluten EN";
    public static final String FAKE_ALLERGEN_PL = "Gluten PL";
    public static final String FAKE_DISH_CATEGORY = "MAIN";
    public static final String FAKE_DISH_NAME = "Pizza";
    public static final String INGREDIENT_EN = "Onion";
    public static final String INGREDIENT_PL = "Cebula";
    public static final String TOKEN_DESSERTS = "DESSERTS";
    public static final String TOKEN_GLUTEN = "GLUTEN";
    public static final String TOKEN_INGREDIENT = "ONION";
    public static final String TOKEN_LACTOSE = "LACTOSE";
    public static final String TOKEN_NUTS = "NUTS";
    public static final String TOKEN_SOUPS = "SOUPS";
    public static final String TOKEN_TOMATO = "TOMATO";
}