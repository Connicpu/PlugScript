module Permissions
  import 'org.bukkit.entity.Player'

  class Providers
    @@vault_plugin = :Vault.plugin

    permission_class = net.milkbowl.vault.permission.Permission.java_class
    permission_provider_service = $context.server.services_manager.get_registration permission_class
    @@permission_provider = permission_provider_service.provider

    chat_class = net.milkbowl.vault.chat.Chat.java_class
    chat_provider_service = $context.server.services_manager.get_registration chat_class
    @@chat_provider = chat_provider_service.provider

    def [](provider)
      case provider
        when :permissions
          @@permission_provider
        when :chat
          @@chat_provider
      end
    end
  end

  @@provider_accessor = Providers.new
  def providers
    @@provider_accessor
  end
  module_function :providers

  def supports_groups
    @providers[:permissions].has_group_support
  end

  def super_perms_compatible
    @providers[:permissions].has_super_perms_compat
  end

  def primary_group(world = nil)
    @perms.get_primary_group world
  end

  class PermissionUser
    attr_reader :player

    @@perms = Permissions.providers[:permissions]
    @@chat = Permissions.providers[:chat]

    def initialize(player)
      raise "the player cannot be nil" if player.nil?
      player = player.name if player.is_a?(Player)
      @player = player
    end

    def groups(world: '')
      @@perms.get_player_groups(world, @player).to_a
    end

    def has(permission, world: '')
      @@perms.has world, @player, permission
    end

    def add(permission, world: '')
      permission = permission.to_s if permission.is_a?(Symbol)
      @@perms.player_add world, @player, permission
    end

    def remove(permission, world: '')
      permission = permission.to_s if permission.is_a?(Symbol)
      @@perms.player_remove world, @player, permission
    end

    def is_in_group?(group, world: '')
      group = group.to_s if group.is_a?(Symbol)
      @@perms.player_in_group 
    end

    def add_group(group, world: '')
      group = group.to_s if group.is_a?(Symbol)
      @@perms.player_add_group world, @player, group
    end

    def remove_group(group, world: '')
      group = group.to_s if group.is_a?(Symbol)
      @@perms.player_remove_group world, @player, group
    end

    def prefix(world: '')
      @@chat.get_player_prefix world, @player
    end

    def prefix=(prefix, world: '')
      @@chat.set_player_prefix world, @player, prefix
    end

    def suffix(world: '')
      @@chat.get_player_suffix world, @player
    end

    def suffix=(suffix, world: '')
      @@chat.set_player_suffix world, @player, suffix
    end

    class PlayerOption
      attr_reader :option, :player, :world
      
      @@chat = Permissions.providers[:chat]

      def initialize(option, player, default, world)
        @option = option
        @player = player
        @default = default
        @world = world
      end

      def string
        default = @default or ''
        @@chat.get_player_info_string @world, @player, @option, default
      end

      def string=(value)
        @@chat.set_player_info_string @world, @player, @option, value
      end

      def boolean
        default = @default or false
        @@chat.get_player_info_boolean @world, @player, @option, default
      end

      def boolean=(value)
        @@chat.set_player_info_boolean @world, @player, @option, value
      end

      def double
        default = @default or 0.0
        @@chat.get_player_info_double @world, @player, @option, default
      end

      def double=(value)
        @@chat.set_player_info_double @world, @player, @option, value
      end

      def integer
        default = @default or 0
        @@chat.get_player_info_integer @world, @player, @option, default
      end

      def integer=(value)
        @@chat.set_player_info_integer @world, @player, @option, value
      end
    end

    def [](option, default: nil, world: '')
      option = option.to_s if option.is_a?(Symbol)
      PlayerOption.new option, @player, default, world
    end
  end

  class PermissionGroup
    attr_reader :group

    @@perms = Permissions.providers[:permissions]
    @@chat = Permissions.providers[:chat]

    def initialize(group)
      group = group.to_s if group.is_a?(Symbol)
      @group = group
    end

    def has(permission, world: '')
      @@perms.group_has world, @group, permission
    end

    def add(permission, world: '')
      @@perms.group_add world, @group, permission
    end

    def remove(permission, world: '')
      @@perms.group_remove world, @group, permission
    end

    def prefix(world: '')
      @@chat.get_group_prefix world, @group
    end

    def prefix=(prefix, world: '')
      @@chat.set_group_prefix world, @group, prefix
    end

    def suffix(world: '')
      @@chat.get_group_suffix world, @group
    end

    def suffix=(suffix, world: '')
      @@chat.set_group_suffix world, @group, suffix
    end

    class GroupOption
      attr_reader :option, :group, :world

      @@chat = Permissions.providers[:chat]

      def initialize(option, group, default, world)
        @option = option
        @group = group
        @default = default
        @world = world
      end

      def string
        default = @default or ''
        @@chat.get_group_info_string @world, @group, @option, default
      end

      def string=(value)
        @@chat.set_group_info_string @world, @group, @option, value
      end

      def boolean
        default = @default or false
        @@chat.get_group_info_boolean @world, @group, @option, default
      end

      def boolean=(value)
        @@chat.set_group_info_boolean @world, @group, @option, value
      end

      def double
        default = @default or 0.0
        @@chat.get_group_info_double @world, @group, @option, default
      end

      def double=(value)
        @@chat.set_group_info_double @world, @group, @option, value
      end

      def integer
        default = @default or 0
        @@chat.get_group_info_integer @world, @group, @option, default
      end

      def integer=(value)
        @@chat.set_group_info_integer @world, @group, @option, value
      end
    end

    def [](option, default: nil, world: '')
      option = option.to_s if option.is_a?(Symbol)
      GroupOption.new option, @group, default, world
    end
  end

  def [](player)
    player = $players[player]
    return nil if player.nil?
    PermissionUser.new player.name
  end

  class GroupAccessor
    def [](group)
      PermissionGroup.new group
    end

    def list
      Permissions.providers[:permissions].groups.to_a
    end

    def entries
      self.list.map {|group| PermissionGroup.new group }
    end

    def to_a
      self.entries
    end

    def each
      self.entries.each do |group|
        yield group
      end
    end
  end

  @@group_accessor = GroupAccessor.new
  def groups
    @@group_accessor
  end

  module_function :supports_groups
  module_function :super_perms_compatible
  module_function :primary_group
  module_function :groups
  module_function :[]
end



