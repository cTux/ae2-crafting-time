package com.ctux.ae2craftingtime.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequesterTtcLayoutTest {
    @Test
    void reservesSpaceOnlyForStatusBars() {
        assertEquals(64, RequesterTtcLayout.statusOffset("request_status_0"));
        assertEquals(64, RequesterTtcLayout.statusOffset("request_status_12"));
        assertEquals(0, RequesterTtcLayout.statusOffset("request_amount_0"));
        assertEquals(0, RequesterTtcLayout.statusOffset(""));
    }

    @Test
    void fitsBesideItemsBelowFieldsAndBeforeStatusBarsOnEveryRow() {
        for (int header : new int[] {19, 20}) {
            for (int row : new int[] {0, 1, 12}) {
                int top = RequesterTtcLayout.rowTop(header, 19, row);
                assertEquals(header + row * 19 + 12, top);
                assertTrue(RequesterTtcLayout.BADGE_X >= 27 + 16);
                for (int width : new int[] {0, 40, 120, 121, 300}) {
                    for (int height : new int[] {0, 9, 10, 16}) {
                        float scale = RequesterTtcLayout.rowScale(width, height);
                        assertTrue(scale > 0 && scale <= 0.5f);
                        assertTrue(RequesterTtcLayout.BADGE_X + Math.ceil(width * scale) + 4
                                < 47 + RequesterTtcLayout.STATUS_OFFSET);
                        assertTrue(top + Math.ceil(height * scale) + 2 <= header + (row + 1) * 19);
                    }
                }
            }
        }
        assertEquals(0.5f, RequesterTtcLayout.rowScale(40, 9));
        assertEquals(0.2f, RequesterTtcLayout.rowScale(300, 9));
        assertEquals(0.3125f, RequesterTtcLayout.rowScale(40, 16));
    }
}
