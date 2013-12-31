package net.connorcpu.plugscript;

import lombok.Data;

/**
 * Created by Connor on 12/30/13.
 */
@Data public class InterpluginMessage {
    private String id;
    private Object[] args;
    private boolean handled = false;
    private Object result = null;
    private String handlingPlugin = null;
}
