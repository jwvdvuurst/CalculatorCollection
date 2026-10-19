import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';

import {
    createAdminCalculator,
    createAdminManufacturer,
    createAdminUser,
    deleteAdminCalculator,
    deleteAdminUser,
    getAdminCalculators,
    getAdminUsers,
    getManufacturers,
    getProposals,
    mergeAdminManufacturers,
    updateAdminCalculator,
    updateAdminManufacturer,
    updateAdminUser,
    updateProposalStatus,
} from './api';
import type {
    AdminCalculator,
    AdminUser,
    CalculatorAdminInput,
    Manufacturer,
    Proposal,
} from './api';

const emptyCalculator: CalculatorAdminInput = {
    model: '',
    manufacturer_id: null,
    sold_from: null,
    sold_to: null,
    source_url: '',
    raw_row_text: '',
    description: '',
    current_price: '0.00',
    price_currency: 'EUR',
};

const errorMessage = (error: unknown) =>
    error instanceof Error ? error.message : 'An unexpected error occurred.';

interface AdminPanelProps {
    currentUsername: string;
}

const AdminPanel = ({ currentUsername }: AdminPanelProps) => {
    const [users, setUsers] = useState<AdminUser[]>([]);
    const [userEdits, setUserEdits] = useState<
        Record<number, Pick<AdminUser, 'email' | 'role' | 'enabled'>>
    >({});
    const [calculators, setCalculators] = useState<AdminCalculator[]>([]);
    const [calculatorSearch, setCalculatorSearch] = useState('');
    const [manufacturers, setManufacturers] = useState<Manufacturer[]>([]);
    const [manufacturerSearch, setManufacturerSearch] = useState('');
    const [proposals, setProposals] = useState<Proposal[]>([]);
    const [calculatorForm, setCalculatorForm] =
        useState<CalculatorAdminInput>(emptyCalculator);
    const [editingCalculatorId, setEditingCalculatorId] = useState<number | null>(null);
    const [userForm, setUserForm] = useState({
        username: '',
        email: '',
        password: '',
        role: 'USER' as 'USER' | 'ADMIN',
    });
    const [manufacturerNames, setManufacturerNames] = useState<Record<number, string>>({});
    const [newManufacturerName, setNewManufacturerName] = useState('');
    const [mergeForm, setMergeForm] = useState({
        target_id: '',
        source_id: '',
        new_name: '',
    });
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState('');
    const [notice, setNotice] = useState('');

    const refresh = async () => {
        const [nextUsers, nextCalculators, nextManufacturers, nextProposals] =
            await Promise.all([
                getAdminUsers(),
                getAdminCalculators(calculatorSearch),
                getManufacturers(),
                getProposals(),
            ]);
        setUsers(nextUsers);
        setUserEdits(
            Object.fromEntries(
                nextUsers.map(({ id, email, role, enabled }) => [
                    id,
                    { email, role, enabled },
                ]),
            ),
        );
        setCalculators(nextCalculators);
        setManufacturers(nextManufacturers);
        setManufacturerNames(
            Object.fromEntries(nextManufacturers.map(({ id, name }) => [id, name])),
        );
        setProposals(nextProposals);
    };

    useEffect(() => {
        void refresh().catch((loadError: unknown) => setError(errorMessage(loadError)));
    }, []);

    const runAction = async (
        action: () => Promise<unknown>,
        successMessage: string,
    ) => {
        setBusy(true);
        setError('');
        setNotice('');
        try {
            await action();
            await refresh();
            setNotice(successMessage);
        } catch (actionError) {
            setError(errorMessage(actionError));
        } finally {
            setBusy(false);
        }
    };

    const saveCalculator = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        await runAction(async () => {
            if (editingCalculatorId === null) {
                await createAdminCalculator(calculatorForm);
            } else {
                await updateAdminCalculator(editingCalculatorId, calculatorForm);
            }
            setCalculatorForm(emptyCalculator);
            setEditingCalculatorId(null);
        }, editingCalculatorId === null ? 'Calculator created.' : 'Calculator updated.');
    };

    const editCalculator = (calculator: AdminCalculator) => {
        setEditingCalculatorId(calculator.id);
        setCalculatorForm({
            ...emptyCalculator,
            model: calculator.model,
            manufacturer_id: calculator.manufacturer_id,
            sold_from: calculator.sold_from,
            sold_to: calculator.sold_to,
            source_url: calculator.source_url,
            raw_row_text: calculator.raw_row_text,
            description: calculator.description,
            current_price: calculator.current_price,
            price_currency: calculator.price_currency,
        });
    };

    const searchCalculators = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        setBusy(true);
        setError('');
        try {
            setCalculators(await getAdminCalculators(calculatorSearch));
        } catch (searchError) {
            setError(errorMessage(searchError));
        } finally {
            setBusy(false);
        }
    };

    const saveUser = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        await runAction(async () => {
            await createAdminUser({ ...userForm, enabled: true });
            setUserForm({ username: '', email: '', password: '', role: 'USER' });
        }, 'User created.');
    };

    const saveUserChanges = async (event: FormEvent<HTMLFormElement>, user: AdminUser) => {
        event.preventDefault();
        const changes = userEdits[user.id];
        if (!changes) {
            return;
        }
        await runAction(
            () => updateAdminUser(user.id, changes),
            `Updated ${user.username}.`,
        );
    };

    const saveManufacturer = async (
        event: FormEvent<HTMLFormElement>,
        manufacturer: Manufacturer,
    ) => {
        event.preventDefault();
        const name = manufacturerNames[manufacturer.id]?.trim();
        if (!name) {
            setError('Manufacturer name is required.');
            return;
        }
        await runAction(
            () => updateAdminManufacturer(manufacturer.id, name),
            `Updated ${manufacturer.name}.`,
        );
    };

    const createManufacturer = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        await runAction(async () => {
            await createAdminManufacturer(newManufacturerName.trim());
            setNewManufacturerName('');
        }, 'Manufacturer created.');
    };

    const mergeManufacturer = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        await runAction(
            () =>
                mergeAdminManufacturers(
                    Number(mergeForm.target_id),
                    Number(mergeForm.source_id),
                    mergeForm.new_name.trim() || null,
                ),
            'Manufacturers merged.',
        );
        setMergeForm({ target_id: '', source_id: '', new_name: '' });
    };

    return (
        <div className="admin-panel">
            {error && <p className="message error" role="alert">{error}</p>}
            {notice && <p className="message success" role="status">{notice}</p>}

            <section className="admin-section">
                <h3>Calculators</h3>
                <form className="filter-panel" onSubmit={searchCalculators}>
                    <label className="field search-field">
                        <span>Find calculators to manage</span>
                        <input
                            value={calculatorSearch}
                            onChange={(event) => setCalculatorSearch(event.target.value)}
                            placeholder="Search model or manufacturer"
                        />
                    </label>
                    <button className="button secondary" type="submit" disabled={busy}>
                        Search
                    </button>
                    <button
                        className="button quiet"
                        type="button"
                        disabled={busy}
                        onClick={() => {
                            setCalculatorSearch('');
                            void getAdminCalculators()
                                .then(setCalculators)
                                .catch((loadError: unknown) => setError(errorMessage(loadError)));
                        }}
                    >
                        Show all
                    </button>
                </form>
                <form className="filter-panel" onSubmit={saveCalculator}>
                    <label className="field">
                        <span>Model</span>
                        <input
                            required
                            value={calculatorForm.model}
                            onChange={(event) =>
                                setCalculatorForm({
                                    ...calculatorForm,
                                    model: event.target.value,
                                })
                            }
                        />
                    </label>
                    <label className="field">
                        <span>Manufacturer</span>
                        <select
                            value={calculatorForm.manufacturer_id ?? ''}
                            onChange={(event) =>
                                setCalculatorForm({
                                    ...calculatorForm,
                                    manufacturer_id: event.target.value
                                        ? Number(event.target.value)
                                        : null,
                                })
                            }
                        >
                            <option value="">No manufacturer</option>
                            {manufacturers.map((manufacturer) => (
                                <option key={manufacturer.id} value={manufacturer.id}>
                                    {manufacturer.name}
                                </option>
                            ))}
                        </select>
                    </label>
                    <label className="field">
                        <span>Price</span>
                        <input
                            type="number"
                            min="0"
                            step="0.01"
                            value={calculatorForm.current_price}
                            onChange={(event) =>
                                setCalculatorForm({
                                    ...calculatorForm,
                                    current_price: event.target.value,
                                })
                            }
                        />
                    </label>
                    <label className="field">
                        <span>Currency</span>
                        <input
                            maxLength={3}
                            value={calculatorForm.price_currency}
                            onChange={(event) =>
                                setCalculatorForm({
                                    ...calculatorForm,
                                    price_currency: event.target.value,
                                })
                            }
                        />
                    </label>
                    <label className="field">
                        <span>Sold from</span>
                        <input
                            type="number"
                            value={calculatorForm.sold_from ?? ''}
                            onChange={(event) =>
                                setCalculatorForm({
                                    ...calculatorForm,
                                    sold_from: event.target.value
                                        ? Number(event.target.value)
                                        : null,
                                })
                            }
                        />
                    </label>
                    <label className="field">
                        <span>Sold to</span>
                        <input
                            type="number"
                            value={calculatorForm.sold_to ?? ''}
                            onChange={(event) =>
                                setCalculatorForm({
                                    ...calculatorForm,
                                    sold_to: event.target.value
                                        ? Number(event.target.value)
                                        : null,
                                })
                            }
                        />
                    </label>
                    <label className="field">
                        <span>Source URL</span>
                        <input
                            type="url"
                            value={calculatorForm.source_url}
                            onChange={(event) =>
                                setCalculatorForm({
                                    ...calculatorForm,
                                    source_url: event.target.value,
                                })
                            }
                        />
                    </label>
                    <label className="field">
                        <span>Description</span>
                        <input
                            value={calculatorForm.description}
                            onChange={(event) =>
                                setCalculatorForm({
                                    ...calculatorForm,
                                    description: event.target.value,
                                })
                            }
                        />
                    </label>
                    <button className="button primary" type="submit" disabled={busy}>
                        {editingCalculatorId === null ? 'Create calculator' : 'Save calculator'}
                    </button>
                    {editingCalculatorId !== null && (
                        <button
                            className="button quiet"
                            type="button"
                            onClick={() => {
                                setEditingCalculatorId(null);
                                setCalculatorForm(emptyCalculator);
                            }}
                        >
                            Cancel edit
                        </button>
                    )}
                </form>
                <div className="table-wrap">
                    <table>
                        <thead>
                            <tr><th>Model</th><th>Manufacturer</th><th>Price</th><th>Actions</th></tr>
                        </thead>
                        <tbody>
                            {calculators.slice(0, 100).map((calculator) => (
                                <tr key={calculator.id}>
                                    <td>{calculator.model}</td>
                                    <td>{calculator.manufacturer ?? 'Unknown'}</td>
                                    <td>{calculator.price_currency} {calculator.current_price}</td>
                                    <td className="actions">
                                        <button
                                            className="button secondary"
                                            type="button"
                                            disabled={busy}
                                            onClick={() => editCalculator(calculator)}
                                        >
                                            Edit
                                        </button>
                                        <button
                                            className="button danger"
                                            type="button"
                                            disabled={busy}
                                            onClick={() =>
                                                void runAction(
                                                    () => deleteAdminCalculator(calculator.id),
                                                    'Calculator deleted.',
                                                )
                                            }
                                        >
                                            Delete
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                    {calculators.length > 100 && (
                        <p className="muted-copy">
                            Showing the first 100 results. Refine the search to locate other
                            entries.
                        </p>
                    )}
                </div>
            </section>

            <section className="admin-section">
                <h3>Manufacturers</h3>
                <form className="filter-panel" onSubmit={createManufacturer}>
                    <label className="field">
                        <span>New manufacturer</span>
                        <input
                            required
                            value={newManufacturerName}
                            onChange={(event) => setNewManufacturerName(event.target.value)}
                        />
                    </label>
                    <button className="button primary" type="submit" disabled={busy}>
                        Create manufacturer
                    </button>
                </form>
                <form className="filter-panel" onSubmit={mergeManufacturer}>
                    <label className="field">
                        <span>Keep manufacturer</span>
                        <select
                            required
                            value={mergeForm.target_id}
                            onChange={(event) =>
                                setMergeForm({ ...mergeForm, target_id: event.target.value })
                            }
                        >
                            <option value="">Choose target</option>
                            {manufacturers.map((item) => (
                                <option key={item.id} value={item.id}>{item.name}</option>
                            ))}
                        </select>
                    </label>
                    <label className="field">
                        <span>Merge this manufacturer into target</span>
                        <select
                            required
                            value={mergeForm.source_id}
                            onChange={(event) =>
                                setMergeForm({ ...mergeForm, source_id: event.target.value })
                            }
                        >
                            <option value="">Choose source</option>
                            {manufacturers.map((item) => (
                                <option key={item.id} value={item.id}>{item.name}</option>
                            ))}
                        </select>
                    </label>
                    <label className="field">
                        <span>Optional target name</span>
                        <input
                            value={mergeForm.new_name}
                            onChange={(event) =>
                                setMergeForm({ ...mergeForm, new_name: event.target.value })
                            }
                        />
                    </label>
                    <button className="button primary" type="submit" disabled={busy}>
                        Merge manufacturers
                    </button>
                </form>
                <label className="field search-field">
                    <span>Find manufacturer to rename</span>
                    <input
                        value={manufacturerSearch}
                        onChange={(event) => setManufacturerSearch(event.target.value)}
                        placeholder="Search manufacturer names"
                    />
                </label>
                <div className="table-wrap">
                    <table>
                        <thead><tr><th>Name</th><th>Calculators</th></tr></thead>
                        <tbody>
                            {manufacturers
                                .filter((manufacturer) =>
                                    manufacturer.name
                                        .toLowerCase()
                                        .includes(manufacturerSearch.trim().toLowerCase()),
                                )
                                .slice(0, 100)
                                .map((manufacturer) => (
                                <tr key={manufacturer.id}>
                                    <td>
                                        <form
                                            className="admin-inline-form"
                                            onSubmit={(event) => saveManufacturer(event, manufacturer)}
                                        >
                                            <input
                                                aria-label={`Name for ${manufacturer.name}`}
                                                value={manufacturerNames[manufacturer.id] ?? ''}
                                                onChange={(event) =>
                                                    setManufacturerNames({
                                                        ...manufacturerNames,
                                                        [manufacturer.id]: event.target.value,
                                                    })
                                                }
                                            />
                                            <button
                                                className="button secondary"
                                                type="submit"
                                                disabled={busy}
                                            >
                                                Save name
                                            </button>
                                        </form>
                                    </td>
                                    <td>{manufacturer.calculator_count}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </section>

            <section className="admin-section">
                <h3>Users</h3>
                <form className="filter-panel" onSubmit={saveUser}>
                    <label className="field">
                        <span>Username</span>
                        <input
                            required
                            value={userForm.username}
                            onChange={(event) =>
                                setUserForm({ ...userForm, username: event.target.value })
                            }
                        />
                    </label>
                    <label className="field">
                        <span>Email</span>
                        <input
                            required
                            type="email"
                            value={userForm.email}
                            onChange={(event) =>
                                setUserForm({ ...userForm, email: event.target.value })
                            }
                        />
                    </label>
                    <label className="field">
                        <span>Temporary password</span>
                        <input
                            required
                            type="password"
                            minLength={12}
                            value={userForm.password}
                            onChange={(event) =>
                                setUserForm({ ...userForm, password: event.target.value })
                            }
                        />
                    </label>
                    <label className="field">
                        <span>Role</span>
                        <select
                            value={userForm.role}
                            onChange={(event) =>
                                setUserForm({
                                    ...userForm,
                                    role: event.target.value as 'USER' | 'ADMIN',
                                })
                            }
                        >
                            <option value="USER">User</option>
                            <option value="ADMIN">Admin</option>
                        </select>
                    </label>
                    <button className="button primary" type="submit" disabled={busy}>
                        Create user
                    </button>
                </form>
                <div className="table-wrap">
                    <table>
                        <thead><tr><th>User</th><th>Email</th><th>Role</th><th>Enabled</th><th>Actions</th></tr></thead>
                        <tbody>
                            {users.map((user) => {
                                const edit = userEdits[user.id] ?? user;
                                return (
                                    <tr key={user.id}>
                                        <td>
                                            {user.username}
                                            {user.username === currentUsername && ' (you)'}
                                        </td>
                                        <td>
                                            <input
                                                aria-label={`Email for ${user.username}`}
                                                type="email"
                                                value={edit.email}
                                                disabled={user.username === currentUsername}
                                                onChange={(event) =>
                                                    setUserEdits({
                                                        ...userEdits,
                                                        [user.id]: { ...edit, email: event.target.value },
                                                    })
                                                }
                                            />
                                        </td>
                                        <td>
                                            <select
                                                aria-label={`Role for ${user.username}`}
                                                value={edit.role}
                                                disabled={user.username === currentUsername}
                                                onChange={(event) =>
                                                    setUserEdits({
                                                        ...userEdits,
                                                        [user.id]: {
                                                            ...edit,
                                                            role: event.target.value as 'USER' | 'ADMIN',
                                                        },
                                                    })
                                                }
                                            >
                                                <option value="USER">User</option>
                                                <option value="ADMIN">Admin</option>
                                            </select>
                                        </td>
                                        <td>
                                            <input
                                                aria-label={`Enabled status for ${user.username}`}
                                                type="checkbox"
                                                checked={edit.enabled}
                                                disabled={user.username === currentUsername}
                                                onChange={(event) =>
                                                    setUserEdits({
                                                        ...userEdits,
                                                        [user.id]: { ...edit, enabled: event.target.checked },
                                                    })
                                                }
                                            />
                                        </td>
                                        <td className="actions">
                                            <form onSubmit={(event) => void saveUserChanges(event, user)}>
                                                <button
                                                    className="button secondary"
                                                    type="submit"
                                                    disabled={busy || user.username === currentUsername}
                                                >
                                                    Save
                                                </button>
                                            </form>
                                            <button
                                                className="button danger"
                                                type="button"
                                                disabled={busy || user.username === currentUsername}
                                                onClick={() =>
                                                    void runAction(
                                                        () => deleteAdminUser(user.id),
                                                        `Deleted ${user.username}.`,
                                                    )
                                                }
                                            >
                                                Delete
                                            </button>
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            </section>

            <section className="admin-section">
                <h3>Proposal review</h3>
                <div className="table-wrap">
                    <table>
                        <thead><tr><th>Manufacturer</th><th>Model</th><th>Submitted by</th><th>Status</th><th>Actions</th></tr></thead>
                        <tbody>
                            {proposals.map((proposal) => (
                                <tr key={proposal.id}>
                                    <td>{proposal.manufacturer_name}</td>
                                    <td>{proposal.model}</td>
                                    <td>{proposal.created_by}</td>
                                    <td>{proposal.status}</td>
                                    <td className="actions">
                                        {proposal.status === 'pending' && (
                                            <>
                                                <button
                                                    className="button primary"
                                                    type="button"
                                                    disabled={busy}
                                                    onClick={() =>
                                                        void runAction(
                                                            () => updateProposalStatus(proposal.id, 'approved'),
                                                            'Proposal approved and calculator created.',
                                                        )
                                                    }
                                                >
                                                    Approve
                                                </button>
                                                <button
                                                    className="button secondary"
                                                    type="button"
                                                    disabled={busy}
                                                    onClick={() =>
                                                        void runAction(
                                                            () => updateProposalStatus(proposal.id, 'rejected'),
                                                            'Proposal rejected.',
                                                        )
                                                    }
                                                >
                                                    Reject
                                                </button>
                                            </>
                                        )}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </section>
        </div>
    );
};

export default AdminPanel;
