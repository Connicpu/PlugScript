require 'java'

java_import "org.bukkit.event.EventPriority"

class CustomHandler
  include_package "net.connorcpu.plugscript"
  include EventHandler

  def new(id, type, priority = EventPriority::NORMAL, ignoreCancelled = true, &handler)
    fullClassName = "org.bukkit.event" + type.join(".")
    fullType = java.lang.Class.forName fullClassName

    @handlerId = id
    @type = fullType
    @priority = priority
    @ignoreCancelled = ignoreCancelled
    @handler = handler
  end

  def self.execute(event)
    @handler.call(self, event) if @handler
  end

  attr_reader :priority
  attr_reader :handlerId
  attr_reader :eventType
  attr_reader :ignoreCancelled
end

# do it just like this, except remove the = before the 
# begin/end, and import the right package.
=begin
  registeration = CustomHandler.new(
    id:   "simple event example",
    type: [:player, :PlayerQuitEvent]
  ) {|event|
    event.quitMessage = "ohi"
  }
=end
