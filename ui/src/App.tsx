import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';

import AdminPanel from './AdminPanel';
import {
    addToCollection,
    addToWishlist,
    getCalculators,
    getCollection,
    getManufacturers,
    getProposals,
    getWishlist,
    login,
    register,
    setAccessToken,
    submitProposal,
    updateProposalStatus,
} from './api';
import type {
    AuthUser,
    Calculator,
    CalculatorFilters,
    CollectionItem,
    Manufacturer,
    Proposal,
    WishlistItem,
} from './api';

const pages = [
    'catalog',
    'collection',
    'wishlist',
    'manufacturers',
    'proposals',
    'admin',
] as const;
type Page = (typeof pages)[number];

const pageLabels: Record<Page, string> = {
    catalog: 'Catalog',
    collection: 'Collection',
    wishlist: 'Wishlist',
    manufacturers: 'Manufacturers',
    proposals: 'Proposals',
    admin: 'Admin',
};

const emptyFilters: CalculatorFilters = {
    search: '',
    manufacturer_id: '',
    min_price: '',
    max_price: '',
    sort: '',
};

const catalogPageSize = 50;

const errorMessage = (error: unknown) =>
    error instanceof Error ? error.message : 'An unexpected error occurred.';

const App = () => {
    const [page, setPage] = useState<Page>('catalog');
    const [catalogPage, setCatalogPage] = useState(0);
    const [authUser, setAuthUser] = useState<AuthUser | null>(null);
    const [authMode, setAuthMode] = useState<'login' | 'register'>('login');
    const [authForm, setAuthForm] = useState({
        username: '',
        email: '',
        password: '',
        confirmPassword: '',
    });
    const [filters, setFilters] = useState<CalculatorFilters>(emptyFilters);
    const [appliedFilters, setAppliedFilters] = useState<CalculatorFilters>(emptyFilters);
    const [calculators, setCalculators] = useState<Calculator[]>([]);
    const [collection, setCollection] = useState<CollectionItem[]>([]);
    const [wishlist, setWishlist] = useState<WishlistItem[]>([]);
    const [manufacturers, setManufacturers] = useState<Manufacturer[]>([]);
    const [proposals, setProposals] = useState<Proposal[]>([]);
    const [loading, setLoading] = useState(true);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState('');
    const [notice, setNotice] = useState('');
    const [refreshKey, setRefreshKey] = useState(0);
    const [proposalForm, setProposalForm] = useState({
        manufacturer_name: '',
        model: '',
        source_url: '',
    });
    const visibleCalculators = calculators.slice(
        catalogPage * catalogPageSize,
        (catalogPage + 1) * catalogPageSize,
    );
    const catalogPageCount = Math.ceil(calculators.length / catalogPageSize);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError('');
        setNotice('');

        const loadPage = async () => {
            if (!authUser) {
                setLoading(false);
                return;
            }
            try {
                switch (page) {
                    case 'catalog': {
                        const [items, makers] = await Promise.all([
                            getCalculators(appliedFilters, controller.signal),
                            getManufacturers(controller.signal),
                        ]);
                        setCalculators(items);
                        setManufacturers(makers);
                        break;
                    }
                    case 'collection':
                        setCollection(await getCollection(authUser.username, controller.signal));
                        break;
                    case 'wishlist':
                        setWishlist(await getWishlist(authUser.username, controller.signal));
                        break;
                    case 'manufacturers':
                        setManufacturers(await getManufacturers(controller.signal));
                        break;
                    case 'proposals':
                        setProposals(await getProposals(controller.signal));
                        break;
                    case 'admin':
                        break;
                }
            } catch (loadError) {
                if (!controller.signal.aborted) {
                    setError(errorMessage(loadError));
                }
            } finally {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            }
        };

        void loadPage();
        return () => controller.abort();
    }, [page, authUser, appliedFilters, refreshKey]);

    const runAction = async (action: () => Promise<unknown>, successMessage: string) => {
        setBusy(true);
        setError('');
        setNotice('');
        try {
            await action();
            setNotice(successMessage);
            setRefreshKey((key) => key + 1);
            return true;
        } catch (actionError) {
            setError(errorMessage(actionError));
            return false;
        } finally {
            setBusy(false);
        }
    };

    const submitCalculatorFilters = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        setCatalogPage(0);
        setAppliedFilters({ ...filters });
    };

    const onSubmitAuth = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        setBusy(true);
        setError('');
        try {
            const response =
                authMode === 'register'
                    ? await register(
                          authForm.username,
                          authForm.email,
                          authForm.password,
                          authForm.confirmPassword,
                      )
                    : await login(authForm.username, authForm.password);
            setAccessToken(response.access_token);
            setAuthUser(response.user);
            setAuthForm({
                username: '',
                email: '',
                password: '',
                confirmPassword: '',
            });
        } catch (authError) {
            setError(errorMessage(authError));
        } finally {
            setBusy(false);
        }
    };

    const onLogout = () => {
        setAccessToken(null);
        setAuthUser(null);
        setPage('catalog');
        setCollection([]);
        setWishlist([]);
        setProposals([]);
    };

    const onSubmitProposal = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        const submitted = await runAction(
            () =>
                submitProposal({
                    ...proposalForm,
                    raw_row_text: 'Submitted from UI',
                }),
            'Proposal submitted.',
        );
        if (submitted) {
            setProposalForm({ manufacturer_name: '', model: '', source_url: '' });
        }
    };

    if (!authUser) {
        return (
            <main className="app-shell">
                <header className="topbar">
                    <a className="brand" href="#login">
                        <span className="brand-mark" aria-hidden="true">CC</span>
                        <span>Calculator<span className="brand-light">Collection</span></span>
                    </a>
                </header>
                <section className="intro">
                    <p className="eyebrow">A home for every calculator</p>
                    <h1>{authMode === 'login' ? 'Welcome back.' : 'Join the collection.'}</h1>
                    <p className="intro-copy">
                        Sign in to explore the catalog and manage your own collection.
                    </p>
                </section>
                <section className="auth-panel">
                    <div className="auth-tabs" role="group" aria-label="Account action">
                        <button
                            className={authMode === 'login' ? 'button primary' : 'button quiet'}
                            type="button"
                            onClick={() => setAuthMode('login')}
                        >
                            Sign in
                        </button>
                        <button
                            className={authMode === 'register' ? 'button primary' : 'button quiet'}
                            type="button"
                            onClick={() => setAuthMode('register')}
                        >
                            Create account
                        </button>
                    </div>
                    {error && <p className="message error" role="alert">{error}</p>}
                    <form className="auth-form" onSubmit={(event) => void onSubmitAuth(event)}>
                        <label className="field">
                            <span>Username</span>
                            <input
                                autoComplete="username"
                                required
                                maxLength={80}
                                value={authForm.username}
                                onChange={(event) =>
                                    setAuthForm({ ...authForm, username: event.target.value })
                                }
                            />
                        </label>
                        {authMode === 'register' && (
                            <label className="field">
                                <span>Email</span>
                                <input
                                    type="email"
                                    autoComplete="email"
                                    required
                                    value={authForm.email}
                                    onChange={(event) =>
                                        setAuthForm({ ...authForm, email: event.target.value })
                                    }
                                />
                            </label>
                        )}
                        <label className="field">
                            <span>Password</span>
                            <input
                                type="password"
                                autoComplete={authMode === 'login' ? 'current-password' : 'new-password'}
                                minLength={authMode === 'register' ? 12 : 1}
                                required
                                value={authForm.password}
                                onChange={(event) =>
                                    setAuthForm({ ...authForm, password: event.target.value })
                                }
                            />
                        </label>
                        {authMode === 'register' && (
                            <label className="field">
                                <span>Confirm password</span>
                                <input
                                    type="password"
                                    autoComplete="new-password"
                                    minLength={12}
                                    required
                                    value={authForm.confirmPassword}
                                    onChange={(event) =>
                                        setAuthForm({
                                            ...authForm,
                                            confirmPassword: event.target.value,
                                        })
                                    }
                                />
                            </label>
                        )}
                        <button className="button primary" type="submit" disabled={busy}>
                            {busy
                                ? 'Please wait…'
                                : authMode === 'login'
                                  ? 'Sign in'
                                  : 'Create account'}
                        </button>
                    </form>
                </section>
                <footer>Calculator Collection <span>·</span> Connected to the Calculator Collector API</footer>
            </main>
        );
    }

    return (
        <main className="app-shell">
            <header className="topbar">
                <a className="brand" href="#catalog" onClick={() => setPage('catalog')}>
                    <span className="brand-mark" aria-hidden="true">CC</span>
                    <span>Calculator<span className="brand-light">Collection</span></span>
                </a>
                <div className="user-field">
                    <span>{authUser.username} · {authUser.role.toLowerCase()}</span>
                    <button className="button quiet" type="button" onClick={onLogout}>
                        Sign out
                    </button>
                </div>
            </header>

            <section className="intro">
                <p className="eyebrow">A home for every calculator</p>
                <h1>Keep your collection in focus.</h1>
                <p className="intro-copy">
                    Explore the catalog, keep track of what you own, and save the models
                    you are still looking for.
                </p>
            </section>

            <nav className="tabs" aria-label="Main navigation">
                {pages
                    .filter((item) => item !== 'admin' || authUser.role === 'ADMIN')
                    .map((item) => (
                    <button
                        key={item}
                        type="button"
                        className={page === item ? 'tab active' : 'tab'}
                        aria-current={page === item ? 'page' : undefined}
                        onClick={() => setPage(item)}
                    >
                        {pageLabels[item]}
                    </button>
                    ))}
            </nav>

            <section className="content">
                <div className="section-heading">
                    <div>
                        <p className="eyebrow">Your workspace</p>
                        <h2>{pageLabels[page]}</h2>
                    </div>
                    {page === 'catalog' && (
                        <span className="result-count">
                            {loading ? 'Loading…' : `${calculators.length} calculators`}
                        </span>
                    )}
                </div>

                {error && <p className="message error" role="alert">{error}</p>}
                {notice && <p className="message success" role="status">{notice}</p>}

                {page === 'catalog' && (
                    <>
                        <form className="filter-panel" onSubmit={submitCalculatorFilters}>
                            <label className="field search-field">
                                <span>Search</span>
                                <input
                                    value={filters.search}
                                    onChange={(event) =>
                                        setFilters({ ...filters, search: event.target.value })
                                    }
                                    placeholder="Model or manufacturer"
                                />
                            </label>
                            <label className="field">
                                <span>Manufacturer</span>
                                <select
                                    value={filters.manufacturer_id}
                                    onChange={(event) =>
                                        setFilters({
                                            ...filters,
                                            manufacturer_id: event.target.value,
                                        })
                                    }
                                >
                                    <option value="">All manufacturers</option>
                                    {manufacturers.map((manufacturer) => (
                                        <option key={manufacturer.id} value={manufacturer.id}>
                                            {manufacturer.name}
                                        </option>
                                    ))}
                                </select>
                            </label>
                            <label className="field price-field">
                                <span>Min price</span>
                                <input
                                    type="number"
                                    min="0"
                                    step="0.01"
                                    value={filters.min_price}
                                    onChange={(event) =>
                                        setFilters({ ...filters, min_price: event.target.value })
                                    }
                                />
                            </label>
                            <label className="field price-field">
                                <span>Max price</span>
                                <input
                                    type="number"
                                    min="0"
                                    step="0.01"
                                    value={filters.max_price}
                                    onChange={(event) =>
                                        setFilters({ ...filters, max_price: event.target.value })
                                    }
                                />
                            </label>
                            <label className="field">
                                <span>Sort by</span>
                                <select
                                    value={filters.sort}
                                    onChange={(event) =>
                                        setFilters({ ...filters, sort: event.target.value })
                                    }
                                >
                                    <option value="">Default</option>
                                    <option value="model_asc">Model A–Z</option>
                                    <option value="model_desc">Model Z–A</option>
                                    <option value="price_asc">Price low to high</option>
                                    <option value="price_desc">Price high to low</option>
                                    <option value="manufacturer_asc">Manufacturer A–Z</option>
                                </select>
                            </label>
                            <button className="button primary filter-button" type="submit">
                                Apply filters
                            </button>
                        </form>
                        <div className="table-wrap">
                            <table>
                                <thead>
                                    <tr>
                                        <th>Model</th>
                                        <th>Manufacturer</th>
                                        <th>Price</th>
                                        <th><span className="visually-hidden">Actions</span></th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {visibleCalculators.map((calculator) => (
                                        <tr key={calculator.id}>
                                            <td className="model-cell">{calculator.model}</td>
                                            <td>{calculator.manufacturer ?? 'Unknown'}</td>
                                            <td>{calculator.price_currency} {calculator.current_price}</td>
                                            <td className="actions">
                                                <button
                                                    className="button secondary"
                                                    type="button"
                                                    disabled={busy}
                                                    onClick={() =>
                                                        void runAction(
                                                            () => addToCollection(authUser.username, calculator.id),
                                                            'Added to collection.',
                                                        )
                                                    }
                                                >
                                                    Add to collection
                                                </button>
                                                <button
                                                    className="button quiet"
                                                    type="button"
                                                    disabled={busy}
                                                    onClick={() =>
                                                        void runAction(
                                                            () => addToWishlist(authUser.username, calculator.id),
                                                            'Added to wishlist.',
                                                        )
                                                    }
                                                >
                                                    Add to wishlist
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                            {!loading && calculators.length === 0 && (
                                <p className="empty-state">No calculators match these filters.</p>
                            )}
                        </div>
                        {calculators.length > 0 && (
                            <div className="pagination">
                                <span>
                                    Showing {catalogPage * catalogPageSize + 1}–
                                    {Math.min((catalogPage + 1) * catalogPageSize, calculators.length)}
                                    {' '}of {calculators.length}
                                </span>
                                <div>
                                    <button
                                        className="button quiet"
                                        type="button"
                                        disabled={catalogPage === 0}
                                        onClick={() => setCatalogPage((current) => current - 1)}
                                    >
                                        Previous
                                    </button>
                                    <button
                                        className="button quiet"
                                        type="button"
                                        disabled={catalogPage >= catalogPageCount - 1}
                                        onClick={() => setCatalogPage((current) => current + 1)}
                                    >
                                        Next
                                    </button>
                                </div>
                            </div>
                        )}
                    </>
                )}

                {page === 'collection' && (
                    <ItemTable
                        loading={loading}
                        items={collection}
                        emptyMessage="No calculators in this collection yet."
                    />
                )}

                {page === 'wishlist' && (
                    <ItemTable
                        loading={loading}
                        items={wishlist}
                        emptyMessage="No calculators in this wishlist yet."
                    />
                )}

                {page === 'manufacturers' && (
                    <div className="table-wrap">
                        <table>
                            <thead>
                                <tr><th>Manufacturer</th><th>Calculators</th></tr>
                            </thead>
                            <tbody>
                                {manufacturers.map((manufacturer) => (
                                    <tr key={manufacturer.id}>
                                        <td className="model-cell">{manufacturer.name}</td>
                                        <td>{manufacturer.calculator_count}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                        {!loading && manufacturers.length === 0 && (
                            <p className="empty-state">No manufacturers found.</p>
                        )}
                    </div>
                )}

                {page === 'proposals' && (
                    <div className="proposal-layout">
                        <form className="proposal-form" onSubmit={(event) => void onSubmitProposal(event)}>
                            <p className="eyebrow">Help us grow the catalog</p>
                            <h3>Suggest a calculator</h3>
                            <label className="field">
                                <span>Manufacturer</span>
                                <input
                                    required
                                    value={proposalForm.manufacturer_name}
                                    onChange={(event) =>
                                        setProposalForm({
                                            ...proposalForm,
                                            manufacturer_name: event.target.value,
                                        })
                                    }
                                />
                            </label>
                            <label className="field">
                                <span>Model</span>
                                <input
                                    required
                                    value={proposalForm.model}
                                    onChange={(event) =>
                                        setProposalForm({ ...proposalForm, model: event.target.value })
                                    }
                                />
                            </label>
                            <label className="field">
                                <span>Source URL <span className="optional">(optional)</span></span>
                                <input
                                    type="url"
                                    value={proposalForm.source_url}
                                    onChange={(event) =>
                                        setProposalForm({
                                            ...proposalForm,
                                            source_url: event.target.value,
                                        })
                                    }
                                />
                            </label>
                            <button className="button primary" type="submit" disabled={busy}>
                                Submit proposal
                            </button>
                        </form>

                        <div className="proposal-list">
                            <h3>Recent proposals</h3>
                            {proposals.length === 0 && !loading ? (
                                <p className="empty-state">No proposals submitted yet.</p>
                            ) : (
                                proposals.map((proposal) => (
                                    <article className="proposal-card" key={proposal.id}>
                                        <div>
                                            <strong>{proposal.model}</strong>
                                            <p>{proposal.manufacturer_name}</p>
                                            <small>Submitted by {proposal.created_by}</small>
                                        </div>
                                        <div className="proposal-actions">
                                            <span className="status">{proposal.status}</span>
                                            {authUser.role === 'ADMIN' && proposal.status === 'pending' && (
                                                <>
                                                    <button
                                                        className="button secondary"
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
                                                        className="button quiet"
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
                                        </div>
                                    </article>
                                ))
                            )}
                        </div>
                    </div>
                )}

                {page === 'admin' && authUser.role === 'ADMIN' && (
                    <AdminPanel currentUsername={authUser.username} />
                )}
            </section>

            <footer>Calculator Collection <span>·</span> Connected to the Calculator Collector API</footer>
        </main>
    );
};

interface ItemTableProps {
    items: CollectionItem[];
    loading: boolean;
    emptyMessage: string;
}

const ItemTable = ({ items, loading, emptyMessage }: ItemTableProps) => (
    <div className="table-wrap">
        <table>
            <thead>
                <tr><th>Model</th><th>Calculator ID</th><th>Notes</th></tr>
            </thead>
            <tbody>
                {items.map((item) => (
                    <tr key={item.calculator_id ?? item.model ?? 'unknown'}>
                        <td className="model-cell">{item.model ?? 'Unknown'}</td>
                        <td>{item.calculator_id ?? '—'}</td>
                        <td>{item.notes || '—'}</td>
                    </tr>
                ))}
            </tbody>
        </table>
        {!loading && items.length === 0 && <p className="empty-state">{emptyMessage}</p>}
    </div>
);

export default App;
