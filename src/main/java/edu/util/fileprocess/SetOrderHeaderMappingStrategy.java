package edu.util.fileprocess;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.text.StrBuilder;

import com.opencsv.CSVReader;
import com.opencsv.ICSVParser;
import com.opencsv.bean.AbstractBeanField;
import com.opencsv.bean.BeanField;
import com.opencsv.bean.BeanFieldDate;
import com.opencsv.bean.BeanFieldPrimitiveTypes;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvDate;
import com.opencsv.bean.MappingStrategy;
import com.opencsv.exceptions.CsvBadConverterException;
import com.opencsv.exceptions.CsvBeanIntrospectionException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

/**A custom mapping strategy which allows both custom header names and custom column placements.<br>
 * Requires both {@code @CsvBindByPosition} and either {@code @CsvBindByName} or {@code @CsvCustomBindByName} applied to each field to set both position and name.*/
public class SetOrderHeaderMappingStrategy<T> implements MappingStrategy<T> {

	Class<? extends T> type;
	Map<String, PropertyDescriptor> descriptors;
	Map<String, BeanField> fieldMap;
	Map<String, Integer> indexMap;
	String[] header;
	/** Locale for error messages. */
    protected Locale errorLocale = Locale.getDefault();
	
	public SetOrderHeaderMappingStrategy(Class<? extends T> beanType) {
		super();
		setType(beanType);
	}
	
	@Override
	public PropertyDescriptor findDescriptor(int col) {
		return descriptors.get(getColName(col));
	}

	@Override
	public BeanField findField(int col) throws CsvBadConverterException {
		return fieldMap.get(getColName(col));
	}

	@Override
	public int findMaxFieldIndex() {
		int max = -1;
		if(fieldMap != null) {
			for(BeanField bf : fieldMap.values()) {
				CsvBindByPosition annot = bf.getField().getAnnotation(CsvBindByPosition.class);
				max = Math.max(max, annot.position());
			}
		}else {
			//need to get fields manually from bean
			for(Field f : loadFields(type)) {
				CsvBindByPosition posAnnot = f.getAnnotation(CsvBindByPosition.class);
				max = Math.max(max, posAnnot.position());
			}
		}
		return max;
	}

	@Override
	public Integer getColumnIndex(String name) {
		if(indexMap == null || indexMap.isEmpty() || !indexMap.containsKey(name)) {
			for(int i = 0; i < header.length; i++) {
				if(name.equalsIgnoreCase(header[i]))
					return i;
			}
			return null;
		}
		return indexMap.get(name);
	}

	@Override
	public boolean isAnnotationDriven() {
		return true;
	}

	@Override
	public void setErrorLocale(Locale errorLocale) {
		this.errorLocale = errorLocale;
	}

	@Override
	public void setType(Class<? extends T> type) throws CsvBadConverterException {
		this.type = type;
		initialize();
	}
    
	/**Called after setting the type.*/
	protected void initialize() {
		//initialize headersOrdered
		header = new String[findMaxFieldIndex() + 1];
		Arrays.fill(header, StringUtils.EMPTY);
		//get headers in their positions based on annotations
		loadFieldMap(); //loadFieldMap gets the column names so this has been added to that
		//load descriptions
		descriptors = new HashMap<>();
		try {
			PropertyDescriptor[] descriptors = Introspector.getBeanInfo(type).getPropertyDescriptors();
			for (PropertyDescriptor descriptor : descriptors) {
	            this.descriptors.put(descriptor.getName(), descriptor);
	        }
		}catch(IntrospectionException e) {
            CsvBeanIntrospectionException csve = new CsvBeanIntrospectionException(
                    ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale).getString("bean.descriptors.uninitialized"));
            csve.initCause(e);
            throw csve;
		}
	}
    
	
	//The below is copied from HeaderColumnNameMappingStrategy and modified to fit this class's purpose

	
	public String getColName(int col) {
		return (null != header && col < header.length) ? header[col] : null;
	}
		
	/**
     * Builds a map of fields for the bean.
     *
     * @throws CsvBadConverterException If there is a problem instantiating the
     *                                  custom converter for an annotated field
     */
    protected void loadFieldMap() throws CsvBadConverterException {
        boolean required;
        fieldMap = new HashMap<>();

        for (Field field : loadFields(this.type)) {
            String columnName;
            String locale;

            // Always check for a custom converter first.
            if (field.isAnnotationPresent(CsvCustomBindByName.class)) {
                CsvCustomBindByName annotation = field.getAnnotation(CsvCustomBindByName.class);
                columnName = annotation.column().trim();
                if(StringUtils.isEmpty(columnName)) {
                    columnName = field.getName();
                }
                Class<? extends AbstractBeanField> converter = field
                        .getAnnotation(CsvCustomBindByName.class)
                        .converter();
                BeanField bean = instantiateCustomConverter(converter);
                bean.setField(field);
                required = annotation.required();
                bean.setRequired(required);
                fieldMap.put(columnName, bean);
            }

            // Otherwise it must be CsvBindByName.
            else {
                CsvBindByName annotation = field.getAnnotation(CsvBindByName.class);
                required = annotation.required();
                columnName = annotation.column().trim();
                locale = annotation.locale();
                if (field.isAnnotationPresent(CsvDate.class)) {
                    String formatString = field.getAnnotation(CsvDate.class).value();
                    if (StringUtils.isEmpty(columnName)) {
                        fieldMap.put(field.getName(),
                                new BeanFieldDate(field, required, formatString, locale, errorLocale));
                    } else {
                        fieldMap.put(columnName, new BeanFieldDate(field, required, formatString, locale, errorLocale));
                    }
                } else {
                    if (StringUtils.isEmpty(columnName)) {
                        fieldMap.put(field.getName(),
                                new BeanFieldPrimitiveTypes(field, required, locale, errorLocale));
                    } else {
                        fieldMap.put(columnName, new BeanFieldPrimitiveTypes(field, required, locale, errorLocale));
                    }
                }
            }
            //Place found column names at correct positions
            CsvBindByPosition posAnnot = field.getAnnotation(CsvBindByPosition.class);
            header[posAnnot.position()] = columnName;
        }
    }
    
    private List<Field> loadFields(Class<? extends T> cls) {
        List<Field> fields = new ArrayList<>();
        for (Field field : FieldUtils.getAllFields(cls)) {
            if ((field.isAnnotationPresent(CsvBindByName.class) || field.isAnnotationPresent(CsvCustomBindByName.class)) 
            		&& field.isAnnotationPresent(CsvBindByPosition.class)) {
                fields.add(field);
            }
        }
        return fields;
    }
    
    /**
     * Attempts to instantiate the class of the custom converter specified.
     *
     * @param converter The class for a custom converter
     * @return The custom converter
     * @throws CsvBadConverterException If the class cannot be instantiated
     */
    protected BeanField instantiateCustomConverter(Class<? extends AbstractBeanField> converter)
            throws CsvBadConverterException {
        try {
            BeanField c = converter.newInstance();
            c.setErrorLocale(errorLocale);
            return c;
        } catch (IllegalAccessException | InstantiationException oldEx) {
            CsvBadConverterException newEx =
                    new CsvBadConverterException(converter,
                            String.format(ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale).getString("custom.converter.invalid"), converter.getCanonicalName()));
            newEx.initCause(oldEx);
            throw newEx;
        }
    }
    
    @Override
    public void captureHeader(CSVReader reader) throws IOException, CsvRequiredFieldEmptyException {
        // Validation
        if(type == null) {
            throw new IllegalStateException(ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale).getString("type.unset"));
        }
        
        // Read the header
        header = ObjectUtils.defaultIfNull(reader.readNext(), ArrayUtils.EMPTY_STRING_ARRAY);

        // Create a list for the Required fields keys.
        List<String> requiredKeys = new ArrayList<>();

        for(Map.Entry<String, BeanField> entrySet : fieldMap.entrySet()) {
            BeanField beanField = entrySet.getValue();
            if (beanField.isRequired()) {
                requiredKeys.add(entrySet.getKey());
            }
        }

        if (requiredKeys.isEmpty()) {
            return;
        }

        // Remove fields that are in the header
        for (int i = 0; i < header.length && !requiredKeys.isEmpty(); i++) {
            requiredKeys.remove(header[i]);
        }

        // Throw an exception if anything is left
        if (!requiredKeys.isEmpty()) {
            StrBuilder builder = new StrBuilder(256);
            String missingRequiredFields = builder.appendWithSeparators(requiredKeys, ",").toString();
            // TODO consider CsvRequiredFieldsEmpty for multiple missing required fields.
            throw new CsvRequiredFieldEmptyException(type, fieldMap.get(requiredKeys.get(0)).getField(),
                    String.format(ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale).getString("header.required.field.absent"),
                            missingRequiredFields));
        }
    }
    
    @Override
	public String[] generateHeader() {
		if(type == null) {
            throw new IllegalStateException(ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale).getString("type.before.header"));
        }
        
        // Always take what's been given or previously determined first.
        if(header == null) {
            // To make testing simpler and because not all receivers are
            // guaranteed to be as flexible with column order as opencsv,
            // make the column ordering deterministic by sorting the column
            // headers alphabetically.
            SortedSet<String> set = new TreeSet<>(fieldMap.keySet());
            header = set.toArray(new String[fieldMap.size()]);
        }
        // Clone so no one has direct access to internal data structures
        return ArrayUtils.clone(header);
	}
    
	@Override
	public void verifyLineLength(int numberOfFields) throws CsvRequiredFieldEmptyException {
		this.errorLocale = ObjectUtils.defaultIfNull(errorLocale, Locale.getDefault());
        
        // It's very possible that setType() was called first, which creates all
        // of the BeanFields, so we need to go back through the list and correct
        // them all.
        if(fieldMap != null) {
            for(BeanField f : fieldMap.values()) {
                f.setErrorLocale(errorLocale);
            }
        }
	}
	
	@Override
	public T createBean() throws InstantiationException, IllegalAccessException {
		if(type == null) {
            throw new IllegalStateException(ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale).getString("type.unset"));
        }
        return type.newInstance();
	}
}