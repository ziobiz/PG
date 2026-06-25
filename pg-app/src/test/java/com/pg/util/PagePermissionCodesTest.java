package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagePermissionCodesTest {

    @Test
    void helloAllowedOnlyForDeleteAndHelloVariants() {
        assertFalse(PagePermissionCodes.helloAllowed("OBSERVER"));
        assertFalse(PagePermissionCodes.helloAllowed("MODIFY"));
        assertTrue(PagePermissionCodes.helloAllowed("DELETE"));
        assertTrue(PagePermissionCodes.helloAllowed("OBSERVER_HELLO"));
        assertTrue(PagePermissionCodes.helloAllowed("MODIFY_HELLO"));
    }

    @Test
    void intersectMergesBaseAndHelloFlag() {
        assertEquals("OBSERVER", PagePermissionCodes.intersect("DELETE", "OBSERVER"));
        assertEquals("OBSERVER_HELLO", PagePermissionCodes.intersect("DELETE", "OBSERVER_HELLO"));
        assertEquals("OBSERVER_HELLO", PagePermissionCodes.intersect("OBSERVER_HELLO", "OBSERVER_HELLO"));
        assertEquals("MODIFY", PagePermissionCodes.intersect("MODIFY_HELLO", "MODIFY"));
        assertEquals("MODIFY_HELLO", PagePermissionCodes.intersect("MODIFY_HELLO", "DELETE"));
    }

    @Test
    void baseMapsHelloVariants() {
        assertEquals("OBSERVER", PagePermissionCodes.base("OBSERVER_HELLO"));
        assertEquals("MODIFY", PagePermissionCodes.base("MODIFY_HELLO"));
    }
}
