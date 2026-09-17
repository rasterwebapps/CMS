package com.cms.service;

import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.cms.config.RazorpayConfig;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

@Component
public class RazorpayOrderGatewayImpl implements RazorpayOrderGateway {

    private final RazorpayConfig config;

    public RazorpayOrderGatewayImpl(RazorpayConfig config) {
        this.config = config;
    }

    @Override
    public String createOrder(long amountPaise, String currency, String receipt) {
        try {
            RazorpayClient client = new RazorpayClient(config.getKeyId(), config.getKeySecret());
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountPaise);
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", receipt);
            orderRequest.put("payment_capture", 1);
            Order order = client.orders.create(orderRequest);
            return order.get("id");
        } catch (RazorpayException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not create payment order", e);
        }
    }
}
