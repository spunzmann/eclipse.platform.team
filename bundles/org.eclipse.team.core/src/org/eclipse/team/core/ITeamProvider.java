package org.eclipse.team.core;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
/**
 * The <code>ITeamProvider</code> interface exposes a basic team model that
 * providers should implement to allow third-party plug-ins to perform team operations
 * programmatically. For example, a code generation tool may want to get source
 * files before generating the code, and check-in the results.  If a team plugin does
 * not adhere to the <i>semantics</i> of the <code>ITeamProvider</code> interface
 * as described below, the provider will not be useable programmatically by other
 * third-party plug-ins.
 * <p>
 * All the methods take a <code>IProgressMonitor</code> parameter that allows the
 * implementation to provide feedback on long running operations (e.g., those involving
 * large resources, multiple resources or numerous server round-trips).  Cancellation is
 * provided by the progress monitor. When a user cancels an operation the resources
 * <em>must</em> be left in a consistent state, even when the operations has not
 * been fully completed.  Implementations should throw the
 * <code>org.eclipse.core.runtime.OperationCanceledException</code> after ensuring
 * the consistent state of the workspace.</p>
 * <p>
 * A number of methods also take a '<code>depth</code>' parameter.  In these methods
 * the <code>depth</code> parameter applies independently to each element of the
 * <code>resources</code> array.  It is used to optimise the case of applying a method to
 * numerous container resources in the same hierarchy.  If the <code>depth</code> value
 * is <code>IResource.DEPTH_ZERO</code> the method is only applied to the given
 * container (and not any resources in that container).  If the depth value is
 * <code>IResource.DEPTH_ONE </code> then the method is applied to all non-container
 * resources in the given container.  If the depth parameter value is
 * <code>IResource.DEPTH_INFINITE</code> then the method is applied to all file
 * resources in the given container and all sub-containers of the given container.</p>
 * <p>
 * <em>Note:</em> The depth parameter values are consistent with their definition in <code>
 * IResource</code>, however, <i>there are currently no API calls that apply to containers, so
 * specifying an operation to <code>IResource.DEPTH_ZERO</code> will not have an effect
 * on any resources.</i>
 * 
 * @see ITeamNature
 * @see ITeamManager
 */
public interface ITeamProvider {

	/**
	 * Updates the local resource to have the same content as the corresponding remote
	 * resource. Where the local resource does not exist, this method will create it.
	 * <p>
	 * If the remote resource is a container (e.g. folder or project) this operation is equivalent 
	 * to getting each non-container member of the remote resource, thereby updating the
	 * content of existing local members, creating local members to receive new remote resources,
	 * and deleting local members that no longer have a corresponding remote resource.</p>
	 * <p>
	 * The method is applied to all resources satisfying the depth parameter, described above.</p>
	 * <p>
	 * Interrupting the method (via the progress monitor) may lead to partial, but consistent, results.</p>
	 * 
	 * @param resources an array of local resources to update from the corresponding remote
	 * resources.
	 * @param depth the depth to traverse the given resources, taken from <code>IResource</code>
	 * static constants.
	 * @param progress a progress monitor to indicate the duration of the operation, or
	 * <code>null</code> if progress reporting is not required.
	 * @throws TeamProviderException if there is a problem getting one or more of the resources.  The
	 * exception will contain multiple statuses, one for each resource in the <code>resources</code>
	 * array.  Possible status codes include:
	 * <ul>
	 * 		<li>NO_REMOTE_RESOURCE</li>
	 *			<li>IO_FAILED</li>
	 * 		<li>NOT_AUTHORIZED</li>
	 * 		<li>UNABLE</li>
	 * </ul>
	 */
	public void get(IResource[] resources, int depth, IProgressMonitor progress) throws TeamException;

	/**
	 * Changes the state of the local resource from checked-in to checked-out and transfers the content
	 * of the remote resource to the local resource.
	 * <p>
	 * Where no corresponding local resource exists in the workspace, one is created (including any
	 * intermediate parent containers) to receive the contents of the remote resource.</p>
	 * <p>
	 * Implementations may optimistically only flag the state change locally and rely on resolving conflicts
	 * during check-in, or they may pessimistically also checkout or lock the remote resource during a
	 * local resource checkout to avoid conflicts.  The provider API does not subscribe to either model
	 * and supports each equally.</p>
	 * <p>
	 * Where checkout is applied to a resource that is already checked-out the method has no
	 * effect.</p>
	 *
	 * @param resources the array of local resources to be checked-out.
	 * @param depth the depth to traverse the given resources, taken from <code>IResource</code>
	 * constants.
	 * @param progress a progress monitor to indicate the duration of the operation, or
	 * <code>null</code> if progress reporting is not required.
	 * @throws TeamProviderException if there is a problem checking-out one or more of the resources.
	 * The exception will contain multiple statuses, one for each resource in the <code>resources</code>
	 * array.  Possible status codes include:
	 * <ul>
	 * 		<li>NOT_CHECKED_IN</li>
	 *			<li>NO_REMOTE_RESOURCE</li>
	 *			<li>IO_FAILED</li>
	 * 		<li>NOT_AUTHORIZED</li>
	 * 		<li>UNABLE</li>
	 * </ul>
	 * @see checkin(IResource[], int, IProgressMonitor)
	 */
	public void checkout(IResource[] resources, int depth, IProgressMonitor progress) throws TeamException;

	/**
	 * Transfers the content of the local resource to the corresponding remote resource, and changes the
	 * state of the local resource from checked-out to checked-in.
	 * <p>
	 * If a remote resource does not exist this method creates a new remote resource with the same content
	 * as the given local resource.  The local resource is said to <i>correspond</i> to the new remote resource.</p>
	 * <p>
	 * Where providers deal with stores that check-out or lock resources this method is an opportunity
	 * to transfer the content and make the corresponding remote check-in or unlock.  It is envisaged that
	 * where the server maintains resource versions, checkin creates a new version of the remote resource.</p>
	 * <p>
	 * Note that some providers may <em>require</em> that a resource is checked-out before it can be
	 * checked-in.  However, all providers must support the explicit checking out a resource before checking
	 * it in (e.g., even if the check out is a no-op).</p>
	 * 
	 * @param resources an array of local resources to be checked-in.
	 * @param the depth to traverse the given resources, taken from <code>IResource</code>
	 * constants.
	 * @param progress a progress monitor to indicate the duration of the operation, or
	 * <code>null</code> if progress reporting is not required.
	 * @throws TeamProviderException if there is a problem checking-in one or more of the resources.
	 * The exception will contain multiple statuses, one for each resource in the <code>resources</code>
	 * array.  Possible status codes include:
	 * <ul>
	 * 		<li>NOT_CHECKED_OUT</li>
	 *			<li>IO_FAILED</li>
	 * 		<li>NOT_AUTHORIZED</li>
	 * 		<li>UNABLE</li>
	 * </ul>
	 * @see checkout(IResource[], int, IProgressMonitor)
	 */
	public void checkin(IResource[] resources, int depth, IProgressMonitor progress) throws TeamException;

	/**
	 * Changes the state of the local resource from checked-out to checked-in without updating the contents
	 * of the remote resource.
	 * <p>
	 * Note that where the provider is a versioning provider, it is envisaged (though not required) that the
	 * uncheckout operation does not create a new version.</p>
	 * <p>
	 * Note also that <code>uncheckout()</code> does not affect the content of the local resource.  The
	 * caller is required to perform a <code>get()</code> to revert the local resource if that is required
	 * (otherwise the local resource will be left with the changes that were made while the remote resource
	 * was checked-out.  Furthermore, it is valid to call <code>uncheckout()</code> with an
	 * <code>IResource</code> that does not exist locally.</p>
	 * 
	 * @param resources an array of the local resources that are to be unchecked-out.
	 * @param depth the depth to traverse the given resources, taken from <code>IResource</code>
	 * constants.
	 * @param progress a progress monitor to indicate the duration of the operation, or
	 * <code>null</code> if progress reporting is not required.
	 * @throws TeamProviderException if there is a problem undoing the check-out of one or more of
	 * the resources.  The exception will contain multiple statuses, one for each resource in the
	 * <code>resources</code> array.  Possible status codes include:
	 * <ul>
	 * 		<li>NOT_CHECKED_OUT</li>
	 *			<li>IO_FAILED</li>
	 * 		<li>NOT_AUTHORIZED</li>
	 * 		<li>UNABLE</li>
	 * </ul>
	 * @see checkin(IResource)
	 * @see uncheckout(IResource)
	 */
	public void uncheckout(IResource[] resources, int depth, IProgressMonitor progress) throws TeamException;

	/**
	 * Deletes the remote resource corresponding to the given local resource.
	 * <p>
	 * The notion of delete is simply to make the remote resource unavailable.  Where the provider
	 * supports versioning it is not specified whether the delete operation makes the version
	 * temporarily or forever unavailable, or indeed whether the entire history is made unavailable.</p>
	 * <p>
	 * Note that the <code>IResource</code>'s passed as arguments may be non-existant in the
	 * workbench, the typical case is when such a resource has been received in a core callback.</p>
	 * <p>
	 * The resource may be checked-in or checked-out prior to deletion.  The local resource is not
	 * deleted by this method.</p>
	 * <p>
	 * Resource deletions are inherently deep.</p>
	 * 
	 * @param resources the array of resources whose corresponding remote resources are to be deleted.
	 * @param progress a progress monitor to indicate the duration of the operation, or
	 * <code>null</code> if progress reporting is not required.
	 * @throws TeamProviderException if there is a problem deleting one or more of
	 * the resources.  The exception will contain multiple statuses, one for each resource in the
	 * <code>resources</code> array.  Possible status codes include:
	 * <ul>
	 * 		<li>NO_REMOTE_RESOURCE</li>
	 *			<li>IO_FAILED</li>
	 * 		<li>NOT_AUTHORIZED</li>
	 * 		<li>UNABLE</li>
	 * </ul>
	 */
	public void delete(IResource[] resources, IProgressMonitor progress) throws TeamException;

	/**
	 * Informs the provider that a local resource's name or path has changed.
	 * <p>
	 * Some providers, such as versioning providers, may require this information to track the resource
	 * across name changes.</p>
	 * <p>
	 * Note that this method is always called <em>after</em> the local resource has been moved.</p>
	 * 
	 * @param source the full name of the resource before it was moved.
	 * @param target the resource that was moved.
	 * @param progress a progress monitor to indicate the duration of the operation, or
	 * <code>null</code> if progress reporting is not required.
	 * @throws TeamProviderException if there is a problem recording the move.  The exception will
	 * contain a single status. Possible status codes are:
	 * <ul>
	 * 		<li>NO_REMOTE_RESOURCE</li>
	 *			<li>IO_FAILED</li>
	 * 		<li>NOT_AUTHORIZED</li>
	 * 		<li>UNABLE</li>
	 * </ul>
	 */	
	public void moved(IPath source, IResource target, IProgressMonitor progress) throws TeamException;
	
	/*
	 * Implementor's Note:
	 * The following methods are required to return promptly (i.e., they may be used to determine the state of
	 * a resource in a UI where long delays are unacceptable).  Implementations may cache these values
	 * and update the cache on an explicit call to #refreshState().
	 * 
	 * They are currently listed in the provider API, however, they may be moved to a new or different
	 * interface in the future to better reflect their UI-orientation.
	 */

	/**
	 * Answers if the remote resource state is checked-out. If the resource has never been checked in this
	 * method will return <code>true</code>.
	 * <p>
	 * It is undefined whether this method tests for a resource being checked out to this workspace
	 * or any workspace.</p>
	 * 
	 * @param resource the local resource to test.
	 * @return <code>true</code> if the resource is checked-out and <code>false</code> if it is not.
	 * @see checkout(IResource[], int, IProgressMonitor)
	 * @see refreshState(IResource[], int, IProgressMonitor)
	 */
	public boolean isCheckedOut(IResource resource);

	/**
	 * Answers whether the resource has a corresponding remote resource.
	 * <p>
	 * Before a resource is checked-in, the resource will occur locally but not remotely, and calls to this
	 * method will return <code>false</code>.  Once a local resource is checked in (and assuming the local
	 * local resource is not moved or the remote resource deleted) there will be a corresponding remote
	 * resource and this method returns <code>true</code>.</p>
	 * 
	 * @param resource the local resource to test.
	 * @return <code>true</code> if the local resource has a corresponding remote resource,
	 * and <code>false</code> otherwise.
	 * @see checkin(IResource[], int, IProgressMonitor)
	 * @see refreshState(IResource[], int, IProgressMonitor)
	 */
	public boolean hasRemote(IResource resource);

	/**
	 * Answer if the local resource currently has a different timestamp to the base timestamp
	 * for this resource.
	 * 
	 * @param resource the resource to test.
	 * @return <code>true</code> if the resource has a different modification
	 * timestamp, and <code>false</code> otherwise.
	 */
	public boolean isDirty(IResource resource);
	
	/**
	 * Answers true if the base of the given resource is different to the
	 * released state of the given resource.
	 */
	public boolean isOutOfDate(IResource resource);
	
	/**
	 * Allows the provider to refresh resource state information for a resource.
	 * <p>
	 * As described above, some state information may be cached by the provider implementation to
	 * avoid server round trips and allow responsive API calls.  Where a caller is relying on this
	 * information being current, they should first explicitly refresh the resouce state.  Of course, there
	 * are no guarantees that the refreshed information will not become stale immediately after the
	 * call to this method.</p>
	 * 
 	 * @param resources the array of local resources to be refreshed.
	 * @param depth the depth to traverse the given resources, taken from <code>IResource</code>
	 * constants.
	 * @param progress a progress monitor to indicate the duration of the operation, or
	 * <code>null</code> if progress reporting is not required.
	 * @throws TeamProviderException if there is a problem refreshing one or more of
	 * the resources.  The exception will contain multiple statuses, one for each resource in the
	 * <code>resources</code> array.  Possible status codes include:
	 * <ul>
	 *			<li>IO_FAILED</li>
	 * 		<li>UNABLE</li>
	 * </ul>
	 */
	public void refreshState(IResource[] resources, int depth, IProgressMonitor progress) throws TeamException;
	
	/*
	 * @see IFileModificationValidator#validateEdit(IFile[], Object)
	 */
	public IStatus validateEdit(IFile[] files, Object context);
	/*
	 * @see IFileModificationValidator#validateSave(IFile)
	 */
	public IStatus validateSave(IFile file);
}