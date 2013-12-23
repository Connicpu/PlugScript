require 'java'

def net
  Java::Net
end

def match_to_enum(str, enum)
  str = str.gsub('_', '').upcase
  enum.values.each { |elem|
      return elem if str == elem.name.gsub('_', '').upcase
  }
  raise StandardError, "Could not match '#{str}' to Enum #{enum}", caller
end

class EventExecutorWrapper
  def initialize(handler)
    @handler = handler
  end

  java_signature 'void execute(org.bukkit.event.Listener, org.bukkit.event.Event)'
  def execute(listener, event)
    @handler.call(event)
  end
end

def register_event(id, type, priority = :normal, ignoreCancelled = false, &handler)
  fullClassName = "org.bukkit.event." + type.join(".")
  fullType = java.lang.Class.forName fullClassName

  eventHandler = net.connorcpu.plugscript.EventHandler.new

  eventHandler.setHandlerId id
  eventHandler.setPriority match_to_enum(priority.to_s, org.bukkit.event.EventPriority)
  eventHandler.setEventType fullType
  eventHandler.setIgnoreCancelled ignoreCancelled

  handlerWrapper = EventExecutorWrapper.new(handler)
  eventHandler.setExecutor handlerWrapper

  $context.registerEvent eventHandler
  return eventHandler
end

class ScriptCommandWrapper
  def initialize(handler, permission, permission_message, usage)
    @handler = handler
    @permission = permission
    @permission_message = permission_message
    @usage = usage
  end

  java_signature 'boolean execute(org.bukkit.command.CommandSender, java.lang.String, java.lang.String[])'
  def execute(sender, label, args)
    if @permission and not sender.hasPermission(@permission)
      if @permission_message
        sender.sendMessage "\u00A7c#{@permission_message}"
      else
        sender.sendMessage "\u00A7cYou do not have permission to do this."
      end
      return true
    end

    result = true
    begin
      result = @handler.call(sender, label, args)
    rescue Exception => e
      puts "Ruby exception: #{e}"
      sender.sendMessage "\u00A7cAn error occurred :("
      return true
    end

    if result == false
      return false
    end
    return true
  end
end

def register_command(data = {}, &handler)
  usage = "\u00A7e" + (data[:usage] || "/<command>")

  handlerWrapper = ScriptCommandWrapper.new(
    handler, 
    data[:permission],
    data[:permission_message],
    usage
  )

  command = net.connorcpu.plugscript.ScriptCommandExecutor.new(
    data[:name],
    data[:description],
    usage,
    data[:aliases] || [],
    handlerWrapper
  )

  $context.registerCommand command
end

def RunnableWrapper
  def initialize(handler)
    @handler = handler
  end

  java_signature 'void run()'
  def run()
    @handler.call()
  end
end

class RubySyncTask < org.bukkit.scheduler.BukkitRunnable
  def initialize(&handler)
    @handler = handler
  end

  java_signature 'void run()'
  def run()
    @handler.call()
  end
end

def run_sync(&handler)
  RubySyncTask.new(handler).runTask($context.getPlugin)
end

def broadcast_message(message)
  run_sync {
    $context.getServer.broadcastMessage message
  }
end


