package co.id.jalin.camunda.training.day4.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RoutingWorker {

    @JobWorker(type = "route-transaction", autoComplete = true)
    public Map<String,Object> routeTransaction(final ActivatedJob job){
        Map<String,Object> vars = job.getVariablesAsMap();
        var issuer = (String) vars.get("issuer");
        if (issuer == null || issuer.isBlank()) {
            vars.put("issuer","BANK_DEFAULT");
        }
        vars.put("routingResult","Routed to: " + issuer);
        return vars;
    }
}
