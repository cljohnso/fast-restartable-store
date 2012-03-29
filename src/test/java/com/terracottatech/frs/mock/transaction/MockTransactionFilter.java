/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.terracottatech.frs.mock.transaction;

import com.terracottatech.frs.mock.recovery.MockAbstractFilter;
import com.terracottatech.frs.recovery.Filter;
import com.terracottatech.frs.action.Action;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author cdennis
 */
public class MockTransactionFilter extends MockAbstractFilter<Action, Action> {

  private final Set<Long> validTransactions = new HashSet<Long>();

  public MockTransactionFilter(Filter<Action> next) {
    super(next);
  }
  
  @Override
  public boolean filter(Action element, long lsn) {
    if (element instanceof MockTransactionCommitAction) {
      validTransactions.add(((MockTransactionCommitAction) element).getId());
      return true;
    } else if (element instanceof MockTransactionBeginAction) {
      validTransactions.remove(((MockTransactionBeginAction) element).getId());
      return true;
    } else if (element instanceof MockTransactionalAction && !validTransactions.contains(((MockTransactionalAction) element).getId())) {
      return false;
    } else {
      return delegate(element, lsn);
    }
  }

  @Override
  protected Action convert(Action element) {
    if (element instanceof MockTransactionalAction) {
      return ((MockTransactionalAction) element).getEmbeddedAction();
    } else {
      return element;
    }
  }
  
}