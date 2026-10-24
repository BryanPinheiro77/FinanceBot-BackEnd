package com.financebot.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

@Component
public class AuthClientIpResolver {
    private final List<IpAddressMatcher> trustedProxies;

    public AuthClientIpResolver(AuthRateLimitProperties properties,
            @Value("${server.forward-headers-strategy:none}") String forwardHeadersStrategy) {
        if (properties.isEnabled() && !"none".equalsIgnoreCase(forwardHeadersStrategy)) {
            throw new IllegalArgumentException("Auth rate limiting exige server.forward-headers-strategy=none");
        }
        trustedProxies = properties.getTrustedProxies().stream().map(this::proxyMatcher).toList();
    }

    public String resolve(HttpServletRequest request) {
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
