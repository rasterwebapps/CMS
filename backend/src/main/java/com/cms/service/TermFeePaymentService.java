package com.cms.service;

import java.util.List;

import com.cms.dto.TermFeePaymentDto;
import com.cms.dto.TermFeePaymentRequest;

import java.time.LocalDate;

public interface TermFeePaymentService {

    TermFeePaymentDto recordPayment(TermFeePaymentRequest request);

    List<TermFeePaymentDto> getPaymentsByDemand(Long demandId);

    TermFeePaymentDto getById(Long id);

    TermFeePaymentDto getPaymentByReceipt(String receiptNumber);

    List<TermFeePaymentDto> getPaymentsByDateRange(LocalDate from, LocalDate to);

    List<TermFeePaymentDto> getPaymentsWithLateFeeByTermInstance(Long termInstanceId);
}
