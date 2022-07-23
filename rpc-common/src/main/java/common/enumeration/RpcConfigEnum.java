package common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum RpcConfigEnum {

    RPC_CONFIG_PATH("rpc.properties"),

    NACOS_ADDRESS("rpc.nacos.address");

    private final String propertyValue;

}
