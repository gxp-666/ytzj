package com.gec.anan.order.repository;


import com.gec.anan.model.entity.order.OrderMonitorRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderMonitorRecordRepository extends MongoRepository<OrderMonitorRecord, String> {

}
