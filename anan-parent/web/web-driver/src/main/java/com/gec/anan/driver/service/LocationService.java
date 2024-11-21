package com.gec.anan.driver.service;

import com.gec.anan.model.form.map.OrderServiceLocationForm;
import com.gec.anan.model.form.map.SearchNearByDriverForm;
import com.gec.anan.model.form.map.UpdateDriverLocationForm;
import com.gec.anan.model.form.map.UpdateOrderLocationForm;
import com.gec.anan.model.vo.map.NearByDriverVo;

import java.util.List;

public interface LocationService {


    Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm);


    Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm);

    Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderLocationServiceFormList);


}
