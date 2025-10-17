package co.id.jalin.camunda.training.day4.worker;

import co.id.jalin.camunda.training.day4.entity.PaymentTransaction;
import co.id.jalin.camunda.training.day4.repository.PaymentTransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.hc.core5.http.ContentType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@Component
@AllArgsConstructor
public class AuthorizationWorker {

    private PaymentTransactionRepository paymentTransactionRepository;

    @JobWorker(type = "authorize-transaction", autoComplete = true)
    public Map<String,Object> authorizeTransaction(final ActivatedJob job) {
        Map<String,Object> vars = job.getVariablesAsMap();
        try {
            var amount = Double.parseDouble(vars.get("amount").toString());
            var cardNumber = (String) vars.get("cardNumber");
            var merchant = (String) vars.get("merchant");
            var issuer = (String) vars.get("issuer");

            Map<String,Object> objInput = new HashMap<>();
            var mapper = new ObjectMapper();
            objInput.put("amount",amount);
            objInput.put("issuer",issuer);
            objInput.put("merchant",merchant);

            var jsonInput = mapper.writeValueAsString(objInput);

            var url = new URL("http://localhost/issuer_api/authorize.php");
            var conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(HttpMethod.POST.name());
            conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                var input = jsonInput.getBytes(StandardCharsets.UTF_8);
                os.write(input,0,input.length);
            }

            var response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            String resp = response.toString();
            vars.put("apiResponse", resp);

            if (resp.contains("APPROVED")) {
                vars.put("status", "APPROVED");
                vars.put("message", "Transaction approved by " + issuer);
            } else if (resp.contains("DECLINED")) {
                vars.put("status", "DECLINED");
                vars.put("message", "Transaction declined by " + issuer);
            } else {
                vars.put("status", "ERROR");
                vars.put("message", "Unknown response from external API");
            }

            PaymentTransaction tx = new PaymentTransaction();
            tx.setProcessInstanceKey(job.getProcessInstanceKey());
            tx.setCardNumber(cardNumber);
            tx.setMerchant(merchant);
            tx.setIssuer(issuer);
            tx.setAmount(amount);
            tx.setStatus((String) vars.get("status"));
            tx.setMessage((String) vars.get("message"));

            paymentTransactionRepository.save(tx);
            log.info("Saved transaction ID: {}", tx.getId());

//            if (amount > 500_000) {
//                vars.put("status","DECLINE");
//                vars.put("message","Transaction declined by: " + issuer);
//            } else {
//                vars.put("status","APPROVE");
//                vars.put("message","Transaction approved by: " + issuer);
//            }

        } catch (Exception e) {
            vars.put("status", "ERROR");
            vars.put("message", "Failed to call external API: " + e.getMessage());
            log.error("Error cause: ",e);
        }
        return vars;
    }
}
