package com.financebot.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

@Component
public class AuthClientIpResolver {
    private final List<IpAddressMatcher> trustedProxies;
    private final byte[] webProxyKey;

    public AuthClientIpResolver(AuthRateLimitProperties properties,
            @Value("${server.forward-headers-strategy:none}") String forwardHeadersStrategy) {
        if (properties.isEnabled() && !"none".equalsIgnoreCase(forwardHeadersStrategy)) {
            throw new IllegalArgumentException("Auth rate limiting exige server.forward-headers-strategy=none");
        }
        String secret = properties.getWebProxySecret();
        webProxyKey = secret == null || secret.isBlank() ? null : Base64.getDecoder().decode(secret);
        if (webProxyKey != null && (webProxyKey.length != 32
                || !Base64.getEncoder().encodeToString(webProxyKey).equals(secret))) {
            throw new IllegalArgumentException("Chave do proxy web deve ser Base64 de 32 bytes");
        }
        trustedProxies = properties.getTrustedProxies().stream().map(this::proxyMatcher).toList();
    }

    public String resolve(HttpServletRequest request) {
        String signedClient = signedClient(request);
        if (signedClient != null) return signedClient;
        String peer = normalize(request.getRemoteAddr());
        if (peer == null) return "unknown";
        if (!trusted(peer)) return peer;

        List<String> chain = new ArrayList<>();
        int length = 0;
        var headers = request.getHeaders("X-Forwarded-For");
        while (headers.hasMoreElements()) {
            String header = headers.nextElement();
            length += header.length();
            if (length > 2048) return peer;
            for (String item : header.split(",", -1)) {
                if (chain.size() >= 20) return peer;
                chain.add(item.trim());
            }
        }
        // Remove somente saltos confiáveis, da conexão imediata para a origem.
        String client = peer;
        for (int index = chain.size() - 1; index >= 0 && trusted(client); index--) {
            client = normalize(chain.get(index));
            if (client == null) return peer;
        }
        return client;
    }

    // A assinatura autentica somente a origem usada na cota, nunca o usuário/JWT.
    private String signedClient(HttpServletRequest request) {
        if (webProxyKey == null) return null;
        String address = singleHeader(request, "X-FinanceBot-Client-IP");
        String timestamp = singleHeader(request, "X-FinanceBot-Proxy-Timestamp");
        String signature = singleHeader(request, "X-FinanceBot-Proxy-Signature");
        String normalized = normalize(address);
        if (normalized == null || timestamp == null || !timestamp.matches("[0-9]{1,12}")
                || signature == null || !signature.matches("[0-9a-f]{64}")) return null;
        long age = Instant.now().getEpochSecond() - Long.parseLong(timestamp);
        if (age < -60 || age > 60) return null;
        String payload = timestamp + "\n" + address + "\n" + request.getMethod() + "\n" + request.getRequestURI();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webProxyKey, "HmacSHA256"));
            byte[] expected = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(expected, HexFormat.of().parseHex(signature)) ? normalized : null;
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Não foi possível validar o proxy web");
        }
    }

    private String singleHeader(HttpServletRequest request, String name) {
        var headers = request.getHeaders(name);
        if (headers == null || !headers.hasMoreElements()) return null;
        String value = headers.nextElement();
        return headers.hasMoreElements() ? null : value;
    }

    private boolean trusted(String address) {
        return trustedProxies.stream().anyMatch(matcher -> matcher.matches(address));
    }

    private IpAddressMatcher proxyMatcher(String cidr) {
        String[] parts = cidr.split("/", -1);
        if (parts.length > 2 || normalize(parts[0]) == null
                || (parts.length == 2 && (!parts[1].matches("[0-9]{1,3}") || Integer.parseInt(parts[1]) == 0))) {
            throw new IllegalArgumentException("Proxy confiável deve ser um IP ou CIDR específico válido");
        }
        return new IpAddressMatcher(cidr);
    }

    private String normalize(String address) {
        if (address == null || address.length() > 45) return null;
        // Aceita apenas literais; nunca resolve nomes enviados pelo cliente via DNS.
        if (address.contains(":")) {
            if (!address.matches("[0-9a-fA-F:.]+")) return null;
        } else {
            String[] octets = address.split("\\.", -1);
            if (octets.length != 4) return null;
            for (String octet : octets) {
                if (!octet.matches("[0-9]{1,3}") || Integer.parseInt(octet) > 255) return null;
            }
        }
        try {
            return InetAddress.getByName(address).getHostAddress();
        } catch (UnknownHostException exception) {
            return null;
        }
    }
}
