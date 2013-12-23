require 'java'

def net; Java::Net; end

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

def register_event(id, type, priority = :normal, ignore_cancelled = false, &handler)
  full_class_name = "org.bukkit.event." + type.join(".")
  full_type = java.lang.Class.forName full_class_name

  event_handler = net.connorcpu.plugscript.EventHandler.new

  event_handler.handler_id       = id
  event_handler.priority         = match_to_enum(priority.to_s, org.bukkit.event.EventPriority)
  event_handler.event_type       = full_type
  event_handler.ignore_cancelled = ignore_cancelled

  handler_wrapper = EventExecutorWrapper.new(handler)
  event_handler.executor = handler_wrapper

  $context.register_event event_handler
  return event_handler
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
    if @permission and not sender.has_permission(@permission)
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

  handler_wrapper = ScriptCommandWrapper.new(
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
    handler_wrapper
  )

  $context.register_command command
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
  RubySyncTask.new { handler.call() }.run_task($context.plugin)
end

def broadcast_message(message)
  run_sync {
    $context.server.broadcast_message message
  }
end

class PlayerAccessor
  def [](name)
    name = name.to_s if name.is_a?(Symbol)

    if name.starts_with?("@")
      return $context.server.get_player_exact name[1,16]
    end

    player = $context.server.get_player_exact name
    if player
      return player
    end
    player = $context.server.get_player name
    if player
      return player
    end

    name.downcase!
    self.each do |player|
      display_name = org.bukkit.ChatColor.strip_color player.display_name.downcase
      if player.name.downcase.starts_with?(name)
        return player
      elsif display_name.downcase.starts_with?(name)
        return player
      end
    end

    return nil
  end

  def each
    $context.server.online_players.each do |player|
      yield player
    end
  end
end

$players = PlayerAccessor.new
def plrs; $players; end

class Symbol
  def plr
    $players[self]
  end
end


