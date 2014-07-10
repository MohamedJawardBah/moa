
package moa.classifiers.multitarget;

import moa.classifiers.AbstractMultiLabelLearner;
import moa.classifiers.Classifier;
import moa.classifiers.MultiTargetRegressor;
import moa.core.DoubleVector;
import moa.core.FastVector;
import moa.core.Measurement;
import moa.options.ClassOption;
import moa.streams.InstanceStream;

import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.MultiLabelInstance;
import com.yahoo.labs.samoa.instances.MultiLabelPrediction;
import com.yahoo.labs.samoa.instances.Prediction;


public class BasicMultiTargetRegressor extends AbstractMultiLabelLearner implements MultiTargetRegressor{


	private static final long serialVersionUID = 1L;

	public ClassOption baseLearnerOption = new ClassOption("baseLearner", 'l',
			"Classifier to train.", Classifier.class, "rules.AMRulesRegressor") ; //rules.AMRules"); 

	protected Classifier[] ensemble;

	protected boolean hasStarted = false;

	@Override
	public void resetLearningImpl() {
		this.hasStarted = false;
	}

	@Override
	public void trainOnInstanceImpl(MultiLabelInstance instance) {
		if (this.hasStarted == false){		
			this.ensemble = new Classifier[instance.numberOutputTargets()];
			Classifier baseLearner = (Classifier) getPreparedClassOption(this.baseLearnerOption);
			baseLearner.resetLearning();
			for (int i = 0; i < this.ensemble.length; i++) {
				this.ensemble[i] = baseLearner.copy();
			}
			this.hasStarted = true;
		}
		for (int i = 0; i < this.ensemble.length; i++) {
			Instance weightedInst = transformInstance(instance,i);
			this.ensemble[i].trainOnInstance(weightedInst);
		}
	}

	protected InstancesHeader[] header;

	protected Instance transformInstance(MultiLabelInstance inst, int outputIndex) {
		if (header == null) {
			this.header = new InstancesHeader[this.ensemble.length];
		}
		if (header[outputIndex] == null) {
			//Create Header
			FastVector attributes = new FastVector();
			for (int attributeIndex = 0; attributeIndex < inst.numInputAttributes(); attributeIndex++) {
				attributes.addElement(inst.inputAttribute(attributeIndex));
			}
			//System.out.println("Number of attributes: "+this.numAttributes+ ","+inst.numAttributes());
			attributes.addElement(inst.outputAttribute(outputIndex));
			this.header[outputIndex] =  new InstancesHeader(new Instances(
					getCLICreationString(InstanceStream.class), attributes, 0));
			this.header[outputIndex].setClassIndex(attributes.size()-1);
			this.ensemble[outputIndex].setModelContext(this.header[outputIndex]);
		}
		//Instance instance = new DenseInstance(this.numAttributes+1);
		//instance.setDataset(dataset[classifierIndex]);
		int numAttributes = this.header[outputIndex].numInputAttributes();
		double[] attVals = new double[numAttributes];
		for (int attributeIndex = 0; attributeIndex < numAttributes; attributeIndex++) {
			attVals[attributeIndex] = inst.valueInputAttribute(attributeIndex);
		}
		Instance instance = new DenseInstance(1.0, attVals);
		instance.setDataset(header[outputIndex]);
		instance.setClassValue(inst.valueOutputAttribute(outputIndex));
		// System.out.println(inst.toString());
		// System.out.println(instance.toString());
		// System.out.println("============");
		return instance;
	}


	@Override
	public boolean isRandomizable() {
		return true;
	}


	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return new Measurement[]{
				new Measurement("perceptron (number)", this.ensemble.length)}; // AC rules + default rule
	}


	@Override
	public void getModelDescription(StringBuilder out, int indent) {
	}

	@Override
	public Prediction getPredictionForInstance(MultiLabelInstance instance) {
		Prediction prediction=null;
		if (this.hasStarted){ 
			prediction=new MultiLabelPrediction(ensemble.length);
			DoubleVector combinedVote = new DoubleVector();
			for (int i = 0; i < this.ensemble.length; i++) {
				prediction.setVote(i, 0, this.ensemble[i].getVotesForInstance(transformInstance(instance,i))[0]);
			}
		}
		
		return prediction;
	}


}


