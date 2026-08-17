package br.com.datum.statuspublisher.controller;

import br.com.datum.statuspublisher.dto.CustomerStatusChangeEvent;
import br.com.datum.statuspublisher.dto.StatusChangeRequest;
import br.com.datum.statuspublisher.service.StatusChangePublisherService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
public class StatusChangeController {

    private final StatusChangePublisherService statusChangePublisherService;

    public StatusChangeController(StatusChangePublisherService statusChangePublisherService) {
        this.statusChangePublisherService = statusChangePublisherService;
    }

    @PostMapping(
            value = "/{id}/status",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CustomerStatusChangeEvent> changeStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody StatusChangeRequest request) {

        CustomerStatusChangeEvent event = statusChangePublisherService.publish(id, request.getStatus());
        return ResponseEntity.accepted().body(event);
    }
}
