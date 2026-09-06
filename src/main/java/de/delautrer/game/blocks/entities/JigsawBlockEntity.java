package de.delautrer.game.blocks.entities;

import de.delautrer.game.nbt.CompoundTag;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.world.World;
import org.joml.Vector3i;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class JigsawBlockEntity extends BlockEntity {

    private String name = "veinstride:entrance";
    private String target = "veinstride:building";
    private String pool = "veinstride:desert_camp/tents";
    private String joint = "rollable";
    private String orientation = "south";
    private String turnsInto = "veinstride:air";

    public JigsawBlockEntity(World world, Vector3i pos) {
        super(world, pos);
    }

    @Override
    public BlockEntityType<?> getType() {
        return BlockEntityTypeRegistry.JIGSAW;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name != null ? name : "veinstride:entrance"; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target != null ? target : "veinstride:building"; }

    public String getPool() { return pool; }
    public void setPool(String pool) { this.pool = pool != null ? pool : "veinstride:desert_camp/tents"; }

    public NamespacedKey getPoolKey() {
        String p = getPool();
        if (p.contains(":")) {
            return NamespacedKey.fromString(p);
        }
        return NamespacedKey.fromString("veinstride:" + p);
    }

    public String getJoint() { return joint; }
    public void setJoint(String joint) { this.joint = "aligned".equalsIgnoreCase(joint) ? "aligned" : "rollable"; }

    public String getOrientation() { return orientation; }
    public void setOrientation(String orientation) { this.orientation = orientation != null ? orientation.toLowerCase() : "south"; }

    public String getTurnsInto() { return turnsInto; }
    public void setTurnsInto(String turnsInto) { this.turnsInto = turnsInto != null ? turnsInto : "veinstride:air"; }

    @Override
    public void writeTag(CompoundTag tag) {
        super.writeTag(tag);
        tag.putString("name", name);
        tag.putString("target", target);
        tag.putString("pool", pool);
        tag.putString("joint", joint);
        tag.putString("orientation", orientation);
        tag.putString("turns_into", turnsInto);
    }

    @Override
    public void readTag(CompoundTag tag) {
        super.readTag(tag);
        if (tag.contains("name")) setName(tag.getString("name"));
        if (tag.contains("target")) setTarget(tag.getString("target"));
        if (tag.contains("pool")) setPool(tag.getString("pool"));
        if (tag.contains("joint")) setJoint(tag.getString("joint"));
        if (tag.contains("orientation")) setOrientation(tag.getString("orientation"));
        if (tag.contains("turns_into")) setTurnsInto(tag.getString("turns_into"));
    }

    @Override
    public void write(DataOutputStream dos) throws IOException {
        super.write(dos);
        dos.writeUTF(name != null ? name : "");
        dos.writeUTF(target != null ? target : "");
        dos.writeUTF(pool != null ? pool : "");
        dos.writeUTF(joint != null ? joint : "");
        dos.writeUTF(orientation != null ? orientation : "");
        dos.writeUTF(turnsInto != null ? turnsInto : "");
    }

    @Override
    public void read(DataInputStream dis) throws IOException {
        super.read(dis);
        setName(dis.readUTF());
        setTarget(dis.readUTF());
        setPool(dis.readUTF());
        setJoint(dis.readUTF());
        setOrientation(dis.readUTF());
        setTurnsInto(dis.readUTF());
    }
}
