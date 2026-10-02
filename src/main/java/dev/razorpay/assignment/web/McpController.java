package dev.razorpay.assignment.web;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/mcp")
public class McpController {
    @GetMapping("/tools") public Map<String,Object> tools() { return Map.of("name","freshdesk-agent-connector","version","1.0","tools", List.of(
        tool("list_tickets","List tickets with page pagination",Map.of("type","object","properties",Map.of("page",Map.of("type","integer","minimum",1),"perPage",Map.of("type","integer","maximum",100)))),
        tool("get_ticket","Get one ticket by id",Map.of("type","object","required",List.of("id"),"properties",Map.of("id",Map.of("type","integer")))),
        tool("search_tickets","Search read-only ticket fields",Map.of("type","object","properties",Map.of("status",Map.of("type","string"),"priority",Map.of("type","string"),"createdAfter",Map.of("type","string","format","date-time"),"query",Map.of("type","string")))),
        tool("get_ticket_conversations","Read a ticket's conversation timeline",Map.of("type","object","required",List.of("ticketId"),"properties",Map.of("ticketId",Map.of("type","integer")))),
        tool("search_contacts","Search contacts by name or email",Map.of("type","object","required",List.of("query"),"properties",Map.of("query",Map.of("type","string"))))) ); }
    private Map<String,Object> tool(String name,String description,Map<String,Object> schema) { return Map.of("name",name,"description",description,"inputSchema",schema,"readOnly",true); }
}
