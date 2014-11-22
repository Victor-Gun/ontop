package it.unibz.krdb.obda.owlrefplatform.core.dagjgrapht;

import it.unibz.krdb.obda.model.OBDAException;
import it.unibz.krdb.obda.ontology.ClassExpression;
import it.unibz.krdb.obda.ontology.DataPropertyExpression;
import it.unibz.krdb.obda.ontology.OClass;
import it.unibz.krdb.obda.ontology.ObjectPropertyExpression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class SemanticIndexCache {

	
	private Map<OClass, SemanticIndexRange> classRanges = new HashMap<>();
	
	private Map<ObjectPropertyExpression, List<Interval>> opeIntervals = new HashMap<>();
	private Map<DataPropertyExpression, List<Interval>> dpeIntervals = new HashMap<>();

	private Map<String, Integer> roleIndexes = new HashMap<>();

	private final TBoxReasoner reasonerDag; 
	
	public SemanticIndexCache(TBoxReasoner reasonerDag) {
		this.reasonerDag = reasonerDag;
		
		//create the indexes		
		SemanticIndexBuilder engine = new SemanticIndexBuilder(reasonerDag);
		
		/*
		 * Creating cache of semantic indexes and ranges
		 */
		for (Entry<ClassExpression, SemanticIndexRange> description : engine.getIndexedClasses()) {
			OClass cdesc = (OClass)description.getKey();
			classRanges.put(cdesc, description.getValue());
		} 
		for (Entry<ObjectPropertyExpression, SemanticIndexRange> description : engine.getIndexedObjectProperties()) {
			int idx = description.getValue().getIndex();
			List<Interval> intervals = description.getValue().getIntervals();

			String iri = description.getKey().getPredicate().getName();
			roleIndexes.put(iri, idx);
			opeIntervals.put(description.getKey(), intervals);
		} 
		for (Entry<DataPropertyExpression, SemanticIndexRange> description : engine.getIndexedDataProperties()) {
			int idx = description.getValue().getIndex();
			List<Interval> intervals = description.getValue().getIntervals();

			String iri = description.getKey().getPredicate().getName();
			roleIndexes.put(iri, idx);
			dpeIntervals.put(description.getKey(), intervals);
		} 
	}

	/***
	 * Returns the index (semantic index) for a class or property. 
	 */

	public int getIndex(OClass c) {
		SemanticIndexRange range = classRanges.get(c);
		
		if (range == null) {
			/* direct name is not indexed, maybe there is an equivalent */
			OClass equivalent = (OClass)reasonerDag.getClassDAG().getVertex(c).getRepresentative();
			range = classRanges.get(equivalent);
		}
				
		return range.getIndex();
	}
	
	public int getIndex(ObjectPropertyExpression p) {
		String name = p.getPredicate().getName();
		Integer index = roleIndexes.get(name);
		
		if (index == null) {
			// direct name is not indexed, maybe there is an equivalent, we need to test
			// with object properties and data properties 
			ObjectPropertyExpression equivalent = reasonerDag.getObjectPropertyDAG().getVertex(p).getRepresentative();
			
			Integer index1 = roleIndexes.get(equivalent.getPredicate().getName());
			
			if (index1 != null)
				return index1;
			
			// TODO: object property equivalent failed, we now look for data property equivalent 
			
			System.out.println(name + " IN " + roleIndexes);
		}
		
		return index;				
	}

	public int getIndex(DataPropertyExpression p) {
		String name = p.getPredicate().getName();
		Integer index = roleIndexes.get(name);
		
		if (index == null) {
			// direct name is not indexed, maybe there is an equivalent, we need to test
			// with object properties and data properties 
			DataPropertyExpression equivalent = reasonerDag.getDataPropertyDAG().getVertex(p).getRepresentative();
			
			Integer index1 = roleIndexes.get(equivalent.getPredicate().getName());
			
			if (index1 != null)
				return index1;
			
			// TODO: object property equivalent failed, we now look for data property equivalent 
			
			System.out.println(name + " IN " + roleIndexes);
		}
		
		return index;				
	}
	
	public void setIndex(OClass concept, int idx) {
		classRanges.get(concept).setIndex(idx);
	}
		
	public void setIndex(ObjectPropertyExpression ope, Integer idx) {
		roleIndexes.put(ope.getPredicate().getName(), idx);
	}

	public void setIndex(DataPropertyExpression dpe, Integer idx) {
		roleIndexes.put(dpe.getPredicate().getName(), idx);
	}
	
	/***
	 * Returns the intervals (semantic index) for a class or property. The String
	 * identifies a class if type = 1, identifies a property if type = 2.
	 * 
	 * @param name
	 * @param i
	 * @return
	 * @throws OBDAException 
	 */
	public List<Interval> getIntervals(OClass concept)  {
		SemanticIndexRange range = classRanges.get(concept);
		if (range == null)
			throw new RuntimeException("Could not create mapping for predicate: " + concept
					+ ". Couldn not find semantic index intervals for the predicate.");
		return range.getIntervals();
	}
	
	public List<Interval> getIntervals(ObjectPropertyExpression ope)  {
		
		List<Interval> intervals = opeIntervals.get(ope);
		if (intervals == null)
			throw new RuntimeException("Could not create mapping for predicate: " + ope
					+ ". Couldn not find semantic index intervals for the predicate.");
		return intervals;
	}

	public List<Interval> getIntervals(DataPropertyExpression dpe)  {
		
		List<Interval> intervals = dpeIntervals.get(dpe);
		if (intervals == null)
			throw new RuntimeException("Could not create mapping for predicate: " + dpe
					+ ". Couldn not find semantic index intervals for the predicate.");
		return intervals;
	}
	
	public void setIntervals(OClass concept, List<Interval> intervals) {
		classRanges.get(concept).setIntervals(intervals);
	}
	
	public void setIntervals(ObjectPropertyExpression ope, List<Interval> intervals) {
		opeIntervals.put(ope, intervals);
	}
	
	public void setIntervals(DataPropertyExpression dpe, List<Interval> intervals) {
		dpeIntervals.put(dpe, intervals);
	}
	
	public void clear() {
		classRanges.clear();
		
		roleIndexes.clear();
		opeIntervals.clear();		
		dpeIntervals.clear();		
	}


	/*
	 * these four methods are used only by SI Repository to save the metadata
	 */
	
	public Set<Entry<OClass, SemanticIndexRange>> getClassIndexEntries() {
		return classRanges.entrySet();
	}
		
	
	
	public Set<Entry<String,Integer>> getRoleIndexEntries() {
		return roleIndexes.entrySet();
	}
	
	
	public Set<Entry<ObjectPropertyExpression, List<Interval>>> getObjectPropertyIntervalsEntries() {
		return opeIntervals.entrySet();
	}
	public Set<Entry<DataPropertyExpression, List<Interval>>> getDataPropertyIntervalsEntries() {
		return dpeIntervals.entrySet();
	}
	
}
