package org.mule.extension.opentracing.api.enums;

public enum TagType {
    component,
    db_instance,
    db_statement,
    db_type,
    db_user,
    error,
    http_method,
    http_status_code,
    http_url,
    message_bus_destination,
    peer_hostname,
    peer_ip,
    peer_port,
    peer_service,
    span_kind;
}
