package co.id.jalin.camunda.training.day4.controller;

import io.camunda.zeebe.client.ZeebeClient;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Log4j2
@AllArgsConstructor
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private ZeebeClient zeebeClient;

    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestBody Map<String,Object> vars){
        try {
            var res = zeebeClient.newCreateInstanceCommand()
                    .bpmnProcessId("BPM_Payment_Switching")
                    .latestVersion()
                    .variables(vars)
                    .send()
                    .join();
            return ResponseEntity.ok(
                    Map.of("processInstanceKey",res.getProcessInstanceKey())
            );
        } catch (Exception e) {
            log.error("Error cause",e);
            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of("message",e.getMessage())
                    );
        }
    }
}
