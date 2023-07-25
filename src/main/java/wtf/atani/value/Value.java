package wtf.atani.value;

import com.google.common.base.Supplier;
import wtf.atani.value.interfaces.ValueChangeListener;
import wtf.atani.value.storage.ValueStorage;

import java.util.ArrayList;

public abstract class Value<T> {
    private final String name, description;
    private final Object owner;
    protected T value;

    private final ArrayList<ValueChangeListener> valueChangeListeners;
    private final ArrayList<Supplier<Boolean>> suppliers;

    public Value(String name, String description, Object owner, T value, ArrayList<ValueChangeListener> valueChangeListeners, ArrayList<Supplier<Boolean>> suppliers) {
        this.name = name;
        this.description = description;
        this.owner = owner;
        this.value = value;

        this.valueChangeListeners = valueChangeListeners;
        this.suppliers = suppliers;

        ValueStorage.getInstance().add(this);
    }

    public Value(String name, String description, Object owner, T value, ValueChangeListener[] valueChangeListeners, Supplier<Boolean>[] suppliers) {
        this.name = name;
        this.description = description;
        this.owner = owner;
        this.value = value;

        ArrayList<ValueChangeListener> valueChangeListenersList = new ArrayList<>();
        ArrayList<Supplier<Boolean>> suppliersList = new ArrayList<>();

        if(valueChangeListeners != null) {
            for(ValueChangeListener valueChangeListener : valueChangeListeners) {
                valueChangeListenersList.add(valueChangeListener);
            }
        }

        if(suppliers != null) {
            for(Supplier<Boolean> supplier : suppliers) {
                suppliersList.add(supplier);
            }
        }

        this.valueChangeListeners = valueChangeListenersList;
        this.suppliers = suppliersList;

        ValueStorage.getInstance().add(this);
    }

    public Value(String name, String description, Object owner, T value) {
        this.name = name;
        this.description = description;
        this.owner = owner;
        this.value = value;

        this.valueChangeListeners = null;
        this.suppliers = null;

        ValueStorage.getInstance().add(this);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Object getOwner() {
        return owner;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        Object curValue = value;
        if(this.valueChangeListeners != null) {
            for(ValueChangeListener valueChangeListener : this.valueChangeListeners) {
                valueChangeListener.onChange(ValueChangeListener.Stage.PRE, this, curValue, value);
            }
        }
        this.value = value;
        if(this.valueChangeListeners != null) {
            for(ValueChangeListener valueChangeListener : this.valueChangeListeners) {
                valueChangeListener.onChange(ValueChangeListener.Stage.POST, this, curValue, value);
            }
        }
    }

    public abstract void setValue(String string);

    public boolean isVisible(){
        if(this.suppliers == null)
            return true;
        else
            for(Supplier<Boolean> booleanSupplier : this.suppliers)
                if(!booleanSupplier.get())
                    return false;
        return true;
    }

    public ArrayList<ValueChangeListener> getValueChangeListeners() {
        return valueChangeListeners;
    }

    public ArrayList<Supplier<Boolean>> getSuppliers() {
        return suppliers;
    }
}
