package it.io.openliberty.guides.multimodules.web;

import org.junit.jupiter.api.Test;
import io.openliberty.guides.multimodules.web.HeightsBean;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HeightsBeanUnitTest {

    @Test
    public void testGetHeightCm() {
        HeightsBean heightBean = new HeightsBean();
        heightBean.setHeightCm("2");
        assertEquals("2", heightBean.getHeightCm());
    }
}