package co.id.jalin.camunda.training.day4.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ValidationWorker {

    @JobWorker(type = "validate-transaction", autoComplete = true)
    public Map<String,Object> validateTransaction(final ActivatedJob job){
        Map<String,Object> vars = job.getVariablesAsMap();
        var amount = Double.parseDouble(vars.get("amount").toString());
        var cardNumber = (String) vars.get("cardNumber");

        if (cardNumber == null || amount <= 0) {
            vars.put("status","DECLINE");
            vars.put("message","Invalid transaction data");
        } else {
            vars.put("status","VALID");
        }
        return vars;
    }
}
