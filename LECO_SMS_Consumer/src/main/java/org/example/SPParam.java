package org.example;

import java.sql.ResultSet;
import java.util.Date;
import java.util.List;

/**
 *
 * @author G-160
 */
public class SPParam {

	/**
	 */
	public static interface Direction {

		public static final int IN=(-1);
		public static final int OUT=(1);
		public static final int INOUT=(0);

	}

	private int direction;
	private int type;
	private Object value;

	/**
	 * @param direction
	 *            IN OUT INOUT
	 * @param type
	 *            {@link oracle.jdbc.driver.OracleTypes}
	 */
	public SPParam(int direction, int type) {
		this.setDirection(direction);
		this.type = type;
	}

	/**
	 * @param direction
	 *            IN OUT INOUT
	 * @param type
	 *            {@link oracle.jdbc.driver.OracleTypes}
	 * @param value
	 */
	public SPParam(int direction, int type, Object value) {
		this.setDirection(direction);
		this.type = type;
		this.value = value;
	}

	/**
	 * Method setDirection.
	 * @param direction Direction
	 */
	public void setDirection(int direction) {
		this.direction = direction;
	}
	/**
	 * Method getValueAsList.
	 * @return List<Map<String,String>>
	 */
	public List getValueAsList() {
		if (value == null)
			return null;
		else if (!(value instanceof List))
			throw new RuntimeException("Unable to cast from " + value.getClass().getName() + " to " + ResultSet.class);
		else
			return (List) value;
	}
	/**
	 * Method getDirection.
	 * @return Direction
	 */
	public int getDirection() {
		return direction;
	}

	/**
	 * @param type
	 *            oracle.jdbc.driver.OracleTypes
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**

	 * @return type oracle.jdbc.driver.OracleTypes */
	public int getType() {
		return type;
	}

	/**
	 * Method setValue.
	 * @param value Object
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * Method getValue.
	 * @return Object
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Method isIn.
	 * @return boolean
	 */
	public boolean isIn() {
		return direction <= 0;
	}

	/**
	 * Method isOut.
	 * @return boolean
	 */
	public boolean isOut() {
		return direction >= 0;
	}

	/**
	 * Method toString.
	 * @return String
	 */
	public String toString() {
		return "SPParam" + "[" + direction + "," + type + "," + valueString(value) + "]";
	}

	/**
	 * Method valueString.
	 * @param value Object
	 * @return String
	 */
	private String valueString(Object value) {
		if (value instanceof Object[]) {
			return arrayToString((Object[]) value);
		}
		return String.valueOf(value);
	}

	/**
	 * Method arrayToString.
	 * @param value Object[]
	 * @return String
	 */
	private String arrayToString(Object[] value) {
		String string = "";
		for (int i = 0; i < value.length; i++) {
			Object object = value[i];
			string += "," + valueString(object);
		}
		return (string + " ").substring(1);
	}

	/**
	 * Method getValueAsMapList.
	 * @return List<Map<String,String>>
	 */
	public List getValueAsMapList() {
		if (value == null)
			return null;
		else if (!(value instanceof List))
			throw new RuntimeException("Unable to cast from " + value.getClass().getName() + " to " + ResultSet.class);
		else
			return (List) value;
	}

	/**
	 * Method getValueAsDate.
	 * @return Date
	 */
	public Date getValueAsDate() {
		if (value instanceof Date) {
			return (Date) value;
		} else {
			throw new RuntimeException("Unable to cast from " + value.getClass().getName() + " to " + Date.class.getName());
		}
	}

	/**
	 * Method getValueAsLong.
	 * @return Long
	 */
	public Long getValueAsLong() {
		if (value == null) {
			return null;
		} else if (value instanceof Number) {
			return new Long(((Number) value).longValue());
		} else if (value instanceof String) {
			return new Long((String) value);
		} else {
			throw new RuntimeException("Unable to cast from " + value.getClass().getName() + " to " + Number.class.getName());
		}
	}

	/**
	 * Method getValueAsDouble.
	 * @return Double
	 */
	public Double getValueAsDouble() {
		if (value == null) {
			return null;
		} else if (value instanceof Number) {
			return new Double(((Number) value).doubleValue());
		} else {
			throw new RuntimeException("Unable to cast from " + value.getClass().getName() + " to " + Number.class.getName());
		}
	}

	/**
	 * Method getValueAsFloat.
	 * @return Float
	 */
	public Float getValueAsFloat() {
		if (value == null) {
			return null;
		} else if (value instanceof Number) {
			return new Float(((Number) value).floatValue());
		} else {
			throw new RuntimeException("Unable to cast from " + value.getClass().getName() + " to " + Number.class.getName());
		}
	}

	/**
	 * Method getValueAsInteger.
	 * @return Integer
	 */
	public Integer getValueAsInteger() {
		if (value == null) {
			return null;
		} else if (value instanceof Number) {
			return new Integer(((Number) value).intValue());
		} else if (value instanceof String) {
			return new Integer((String) value);
		} else {
			throw new RuntimeException("Unable to cast from " + value.getClass().getName() + " to " + Number.class.getName());
		}
	}

	/**
	 * Method getValueAsShort.
	 * @return Short
	 */
	public Short getValueAsShort() {
		if (value == null) {
			return null;
		} else if (value instanceof Number) {
			return new Short(((Number) value).shortValue());
		} else if (value instanceof String) {
			return new Short((String) value);
		} else {
			throw new RuntimeException("Unable to cast from " + value.getClass().getName() + " to " + Number.class.getName());
		}
	}

	/**
	 * Method getValueAsString.
	 * @return String
	 */
	public String getValueAsString() {
		if (value == null) {
			return null;
		} else {
			return value.toString();
		}
	}

	/**
	 * Method getValueAsCharacter.
	 * @return Character
	 */
	public Character getValueAsCharacter() {
		if (value instanceof Character) {
			return (Character) value;
		} else {
			throw new RuntimeException("Unable to cast from " + value.getClass().getName() + " to " + Character.class.getName());
		}
	}
}
